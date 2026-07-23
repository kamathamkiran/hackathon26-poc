package com.db.hackathon.adk.agent.verification;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.adk.agent.extraction.SemanticRunnerService;
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

    private final SemanticRunnerService runnerService;
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
        log.info("Credit agreement critic checked {} field(s) {} for workflow {}",
                evidence.size(),
                evidence.stream().map(FieldEvidence::path).toList(),
                context.getWorkflow().getWorkflowId());
        log.debug("Credit agreement critic raw response for workflow {}:\n{}",
                context.getWorkflow().getWorkflowId(), raw);
        CreditAgreementVerificationResult result = parseResult(raw, evidence);
        context.setCreditAgreementVerificationResult(result);
        log.info("Credit agreement verification result for workflow {}: {}",
                context.getWorkflow().getWorkflowId(),
                result);

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
            evidence.add(new FieldEvidence("document", "", "", ""
            ,"Missing document pages for verification"));
            return evidence;
        }
        collectLeafEvidence(deal, "", document, evidence);
        return evidence;
    }

    private void collectLeafEvidence(JsonNode node, String path,
                                     DocumentAnalysis document, List<FieldEvidence> evidence) {
        if (isExtractedField(node)) {
            addFieldEvidence(node, path, document, evidence);
            return;
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                String fieldPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
                collectLeafEvidence(field.getValue(), fieldPath, document, evidence);
            }
            return;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                collectLeafEvidence(node.get(i), path + "[" + i + "]", document, evidence);
            }
        }
    }

    /** An extracted leaf is the metadata wrapper: value, pageNumber, confidence, sourceText. */
    private boolean isExtractedField(JsonNode node) {
        return node != null && node.isObject()
                && node.has("value") && node.has("pageNumber");
    }

    private void addFieldEvidence(JsonNode field, String path,
                                  DocumentAnalysis document, List<FieldEvidence> evidence) {
        JsonNode valueNode = field.path("value");
        String value = valueNode.isNull() ? "" : valueNode.asText("");
        if (value.isBlank()) {
            // Nothing was extracted for this field, so there is nothing to verify against the PDF.
            return;
        }

        JsonNode pageNode = field.path("pageNumber");
        if (pageNode.isMissingNode() || pageNode.isNull() || !pageNode.canConvertToInt()) {
            evidence.add(new FieldEvidence(path, value, "", "",
                    "Missing page number for field"));
            return;
        }

        int pageNumber = pageNode.asInt();
        String sourceText = field.path("sourceText").asText("");
        try {
            evidence.add(new FieldEvidence(path, value, "page " + pageNumber,
                    excerpt(document, pageNumber, sourceText), null));
        } catch (RuntimeException exception) {
            evidence.add(new FieldEvidence(path, value, "page " + pageNumber, sourceText,
                    "Cited page " + pageNumber + " could not be resolved"));
        }
    }

    private String excerpt(DocumentAnalysis document, int pageNumber, String sourceText) {
        DocumentPage page = document.getPages().stream()
                .filter(candidate -> candidate.getPageNumber() != null
                        && candidate.getPageNumber() == pageNumber)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Page not found: " + pageNumber));

        String pageText = page.getText() == null ? "" : page.getText();
        StringBuilder result = new StringBuilder();
        if (sourceText != null && !sourceText.isBlank()) {
            result.append("[cited source text] ").append(sourceText.trim()).append('\n');
        }
        result.append("[page ").append(pageNumber).append("]\n").append(pageText);
        return result.toString().trim();
    }

    private String buildPrompt(String json, List<FieldEvidence> evidence) {
        StringBuilder prompt = new StringBuilder()
                .append("You are a strict credit-agreement critic.\n")
                .append("Compare each JSON field only with its cited PDF excerpt.\n")
                .append("Do not infer missing values or repair placeholders.\n")
                .append("Check exactly the ").append(evidence.size())
                .append(" fields listed below - no more, no less. ")
                .append("Echo every field path you checked in 'checkedFieldPaths'.\n")
                .append("Return JSON only in this shape: ")
                .append("{\"decision\":\"SUCCESS|FAILURE\",\"checkedFields\":0,"
                        + "\"checkedFieldPaths\":[\"...\"],"
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

    private CreditAgreementVerificationResult parseResult(String raw, List<FieldEvidence> evidence)
            throws Exception {
        int expectedFields = evidence.size();
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
            mismatches.add(describeFieldCountMismatch(checkedFields, expectedFields,
                    evidence, result.path("checkedFieldPaths")));
        }
        if (!decision.equals("SUCCESS") && !decision.equals("FAILURE")) {
            mismatches.add("Critic returned an invalid decision: '" + decision + "'");
        }
        if (decision.equals("FAILURE") && mismatchArray.isArray() && mismatchArray.isEmpty()) {
            mismatches.add("Critic returned FAILURE without explaining a mismatch");
        }

        return CreditAgreementVerificationResult.builder()
                .verified(decision.equals("SUCCESS") && mismatches.isEmpty())
                .checkedFields(checkedFields)
                .mismatches(mismatches)
                .build();
    }

    /**
     * Builds a human-readable explanation of why the critic's field count differs from the number
     * of fields we submitted, listing the exact paths that were extra or missing.
     */
    private String describeFieldCountMismatch(int checkedFields, int expectedFields,
                                              List<FieldEvidence> evidence, JsonNode checkedPathsNode) {
        List<String> expectedPaths = evidence.stream().map(FieldEvidence::path).toList();
        StringBuilder message = new StringBuilder("Critic checked ")
                .append(checkedFields).append(" fields, expected ").append(expectedFields);

        if (checkedPathsNode != null && checkedPathsNode.isArray()) {
            List<String> checkedPaths = new ArrayList<>();
            checkedPathsNode.forEach(node -> checkedPaths.add(node.asText()));

            List<String> extra = new ArrayList<>(checkedPaths);
            extra.removeAll(expectedPaths);
            List<String> missing = new ArrayList<>(expectedPaths);
            missing.removeAll(checkedPaths);

            if (!extra.isEmpty()) {
                message.append("; unexpected fields checked by critic: ").append(extra);
            }
            if (!missing.isEmpty()) {
                message.append("; fields the critic skipped: ").append(missing);
            }
        } else {
            message.append("; fields submitted for review: ").append(expectedPaths);
        }
        return message.toString();
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
}
