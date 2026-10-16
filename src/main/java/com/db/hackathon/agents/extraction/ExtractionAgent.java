package com.db.hackathon.agents.extraction;

import com.db.hackathon.agents.WorkflowAgent;
import com.db.hackathon.agents.review.DealNormalizer;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.dto.ExtractionResponse;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.service.PdfSourceService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionAgent implements WorkflowAgent {

    private final SemanticRunnerService runnerService;
    private final PdfSourceService pdfSourceService;
    private final DealNormalizer dealNormalizer;

    @Override
    public AgentType getAgentType() {
        return AgentType.EXTRACTION;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.VALIDATING;
    }

    @Override
    public void process(
            WorkflowContext context) throws Exception {

        log.info("Starting agreement extraction");

        SemanticRunnerService.AgentResult<JsonNode> result =
                runnerService.extractAgreement(pdfSourceService.loadPdf(context));

        context.setDeal(dealNormalizer.normalize(result.value()));
        context.setAgentTokenUsage(result.tokenUsage());

        log.info("Agreement extraction completed");

    }

    @Override
    public Object getOutput(WorkflowContext context) {
        return ExtractionResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .deal(context.getDeal())
                .build();
    }

}
