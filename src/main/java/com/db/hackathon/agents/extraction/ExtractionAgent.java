package com.db.hackathon.agents.extraction;

import com.db.hackathon.agents.WorkflowAgent;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.dto.ExtractionResponse;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.service.PdfSourceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionAgent implements WorkflowAgent {

    private final SemanticRunnerService runnerService;
    private final PdfSourceService pdfSourceService;

    @Override
    public AgentType getAgentType() {
        return AgentType.EXTRACTION;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.EXTRACTED;
    }

    @Override
    public void process(
            WorkflowContext context) throws Exception {

        log.info("Starting agreement extraction");

        Deal deal = runnerService.extractAgreement(pdfSourceService.loadPdf(context));

        context.setDeal(deal);

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
