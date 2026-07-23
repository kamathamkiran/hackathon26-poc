package com.db.hackathon.adk.agent.extraction;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.model.extraction.Deal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionAgent implements WorkflowAgent {

    private final SemanticRunnerService runnerService;

    @Override
    public AgentType getAgentType() {
        return AgentType.EXTRACTION;
    }

    @Override
    public WorkflowStatus getInputStatus() {
        return WorkflowStatus.EXTRACTING;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.EXTRACTED;
    }

    @Override
    public void process(
            WorkflowContext context) throws Exception {

        log.info("Starting agreement extraction");

        Deal deal = runnerService.extractAgreement(
                        context.getDocumentAnalysis());

        context.setDeal(deal);
        log.info("Extracted deal data: {}", deal);

        log.info("Agreement extraction completed");

    }

    @Override
    public Object getOutput(WorkflowContext context) {
        return context.getDeal();
    }

}
