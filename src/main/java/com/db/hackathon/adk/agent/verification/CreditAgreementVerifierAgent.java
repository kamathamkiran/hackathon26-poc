package com.db.hackathon.adk.agent.verification;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.adk.agent.extraction.AdkRunnerService;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.exception.WorkflowVerificationException;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.document.DocumentPage;
import com.db.hackathon.model.verification.CreditAgreementVerificationResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/** Uses cited page/line ranges and an LLM to verify extracted values in the PDF. */
@Component
@RequiredArgsConstructor
@Slf4j
public class CreditAgreementVerifierAgent implements WorkflowAgent {

    private final AdkRunnerService runnerService;
    private final ObjectMapper objectMapper;

    @Override
    public AgentType getAgentType() {
        return AgentType.CREDIT_AGREEMENT_VERIFIER;
    }

    @Override
    public WorkflowStatus getInputStatus() {
        return WorkflowStatus.DEAL_DATA_VERIFIED;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.CREDIT_AGREEMENT_VERIFIED;
    }

    @Override
    public void process(WorkflowContext context) throws Exception {
        String json = context.getDealDataJson();
        if (json == null || json.isBlank()) {
            throw new WorkflowVerificationException("No deal JSON was available for credit agreement verification");
        }

        JsonNode deal = objectMapper.readTree(json);
        List<FieldEvidence> evidence = collectEvidence(deal, context.getDocumentAnalysis());
        List<String> evidenceIssues = evidence.stream()
                .filter(FieldEvidence::hasIssue)
                .map(FieldEvidence::issue)
                .toList();

        if (!evidenceIssues.isEmpty()) {
            setFailure(context, evidence.size(), evidenceIssues);
            throw new WorkflowVerificationException(
                    "Credit agreement evidence could not be resolved: "
                            + String.join("; ", evidenceIssues));
        }

        String raw = runnerService.runPrompt(buildPrompt(json, evidence));
        CreditAgreementVerificationResult result = parseResult(raw, evidence.size());
        context.setCreditAgreementVerificationResult(result);

        if (!result.isVerified()) {
            throw new WorkflowVerificationException(
                    "Credit agreement verification failed: "
                            + String.join("; ", result.getMismatches()));
        }

        log.info("Credit agreement verification passed for workflow {}",
                context.getWorkflow().getWorkflowId());
    }

    @Override
    public Object getOutput(WorkflowContext context) {
        return context.getCreditAgreementVerificationResult();
    }

    private List<FieldEvidence> collectEvidence(JsonNode deal, DocumentAnalysis document) {
        List<FieldEvidence> evidence = new ArrayList<>();
        if (document == null || document.getPages() == null) {
            evidence.add(new FieldEvidence("document", "", "", "",
                    "Document analysis is unavailable"));
            return evidence;
        }
        collectLeafEvidence(deal, deal, "", document, evidence);
        return evidence;
    }

    private void collectLeafEvidence(JsonNode root, JsonNode node, String path,
                                     DocumentAnalysis document, List<FieldEvidence> evidence) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (path.isEmpty() && field.getKey().equals("_sourceLocations")) {
                    continue;
                }
                String fieldPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
                collectLeafEvidence(root, field.getValue(), fieldPath, document, evidence);
            }
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectLeafEvidence(root, node.get(i), path + "[" + i + "]", document, evidence);
            }
            return;
        }

        JsonNode rangeNode = root.path("_sourceLocations").get(path);
        if (rangeNode == null || !rangeNode.isTextual()) {
            evidence.add(new FieldEvidence(path, node.toString(), "", "",
                    "Missing page/line range for field"));
            return;
        }

        String range = rangeNode.asText();
        try {
            evidence.add(new FieldEvidence(path, node.toString(), range,
                    excerpt(document, PageLineRange.parse(range))));
        } catch (RuntimeException exception) {
            evidence.add(new FieldEvidence(path, node.toString(), range, "",
                    "Cannot read cited range: " + exception.getMessage()));
        }
    }

    private String excerpt(DocumentAnalysis document, PageLineRange range) {
        StringBuilder result = new StringBuilder();
        for (int pageNumber = range.startPage(); pageNumber <= range.endPage(); pageNumber++) {
            DocumentPage page = document.getPages().stream()
                    .filter(candidate -> candidate.getPageNumber() != null
                            && candidate.getPageNumber() == pageNumber)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageNumber));

            String[] lines = (page.getText() == null ? "" : page.getText()).split("\\R", -1);
            int firstLine = pageNumber == range.startPage() ? range.startLine() : 1;
            int lastLine = pageNumber == range.endPage() ? range.endLine() : lines.length;
            if (firstLine < 1 || lastLine < firstLine || lastLine > lines.length) {
                throw new IllegalArgumentException("Line range not found on page " + pageNumber);
            }

            for (int line = firstLine; line <= lastLine; line++) {
                result.append("[page ").append(pageNumber).append(", line ").append(line)
                        .append("] ").append(lines[line - 1]).append('\n');
            }
        }
        return result.toString().trim();
    }

    private String buildPrompt(String json, List<FieldEvidence> evidence) {
        StringBuilder prompt = new StringBuilder()
                .append("You are a strict credit-agreement critic.\n")
                .append("Compare each JSON field only with its cited PDF excerpt.\n")
                .append("Do not infer missing values or repair placeholders.\n")
                .append("Return JSON only in this shape: ")
                .append("{\"decision\":\"SUCCESS|FAILURE\",\"checkedFields\":0,"
                        + "\"mismatches\":[{\"fieldPath\":\"...\",\"reason\":\"...\"}]}\n\n")
                .append("DEAL JSON:\n").append(json).append("\n\n");

        for (FieldEvidence field : evidence) {
            prompt.append("FIELD: ").append(field.path()).append('\n')
                    .append("DEAL VALUE: ").append(field.value()).append('\n')
                    .append("CITED RANGE: ").append(field.range()).append('\n')
                    .append("PDF EXCERPT:\n").append(field.excerpt()).append("\n\n");
        }
        return prompt.toString();
    }

    private CreditAgreementVerificationResult parseResult(String raw, int expectedFields) throws Exception {
        int start = raw.indexOf('{');
        int end = raw.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw new WorkflowVerificationException("Credit agreement critic did not return JSON");
        }

        JsonNode result = objectMapper.readTree(raw.substring(start, end + 1));
        String decision = result.path("decision").asText("");
        int checkedFields = result.path("checkedFields").asInt(-1);
        List<String> mismatches = new ArrayList<>();
        JsonNode mismatchArray = result.path("mismatches");
        if (!mismatchArray.isArray()) {
            mismatches.add("Critic response is missing the mismatches array");
        } else {
            for (JsonNode mismatch : mismatchArray) {
                mismatches.add(mismatch.path("fieldPath").asText("unknown field") + ": "
                        + mismatch.path("reason").asText("unspecified mismatch"));
            }
        }
        if (checkedFields != expectedFields) {
            mismatches.add("Critic checked " + checkedFields + " fields, expected " + expectedFields);
        }
        if (!decision.equals("SUCCESS") && !decision.equals("FAILURE")) {
            mismatches.add("Critic returned an invalid decision: " + decision);
        }
        if (decision.equals("FAILURE") && mismatchArray.isArray() && mismatchArray.size() == 0) {
            mismatches.add("Critic returned FAILURE without explaining a mismatch");
        }

        return CreditAgreementVerificationResult.builder()
                .verified(decision.equals("SUCCESS") && mismatches.isEmpty())
                .checkedFields(checkedFields)
                .mismatches(mismatches)
                .build();
    }

    private void setFailure(WorkflowContext context, int checkedFields, List<String> issues) {
        context.setCreditAgreementVerificationResult(CreditAgreementVerificationResult.builder()
                .verified(false)
                .checkedFields(checkedFields)
                .mismatches(issues)
                .build());
    }

    private record FieldEvidence(String path, String value, String range,
                                  String excerpt, String issue) {
        private boolean hasIssue() {
            return issue != null && !issue.isBlank();
        }
    }

    private record PageLineRange(int startPage, int startLine, int endPage, int endLine) {
        private static PageLineRange parse(String value) {
            String[] parts = value.split("[-,]");
            if (parts.length != 4) {
                throw new IllegalArgumentException("Invalid page/line range: " + value);
            }

            try {
                PageLineRange range = new PageLineRange(
                        Integer.parseInt(parts[0]), Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
                if (range.startPage() < 1 || range.startLine() < 1
                        || range.endPage() < range.startPage()
                        || (range.endPage() == range.startPage()
                        && range.endLine() < range.startLine())) {
                    throw new IllegalArgumentException("Invalid page/line range: " + value);
                }
                return range;
            } catch (NumberFormatException exception) {
                throw new IllegalArgumentException("Invalid page/line range: " + value, exception);
            }
        }
    }
}
