package com.db.hackathon.workflow;

import com.db.hackathon.adk.agent.document.DocumentParserAgent;
import com.db.hackathon.adk.agent.extraction.ExtractionAgent;
import com.db.hackathon.adk.agent.validation.ValidationAgent;
import com.db.hackathon.adk.agent.verification.CreditAgreementVerifierAgent;
import com.db.hackathon.adk.agent.verification.DealDataVerifierAgent;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.entity.WorkflowEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private final WorkflowContextBuilder contextBuilder;
    private final WorkflowExecutor executor;

    private final DocumentParserAgent parserAgent;
    private final ExtractionAgent extractionAgent;
    private final ValidationAgent validationAgent;
    private final DealDataVerifierAgent dealDataVerifierAgent;
    private final CreditAgreementVerifierAgent creditAgreementVerifierAgent;

    public WorkflowContext execute(WorkflowEntity workflow) {

        WorkflowContext context = contextBuilder.build(workflow);

        try {
            log.info("Executing workflow: {}", workflow.getWorkflowId());

            switch (workflow.getStatus()) {

                case UPLOADED -> {
                    executor.execute(parserAgent, context);
                    executor.execute(extractionAgent, context);
                    executor.execute(validationAgent, context);
                    executor.execute(dealDataVerifierAgent, context);
                    executor.execute(creditAgreementVerifierAgent, context);
                }

                case PARSED -> {
                    executor.execute(extractionAgent, context);
                    executor.execute(validationAgent, context);
                    executor.execute(dealDataVerifierAgent, context);
                    executor.execute(creditAgreementVerifierAgent, context);
                }

                case EXTRACTED -> {
                    executor.execute(validationAgent, context);
                    executor.execute(dealDataVerifierAgent, context);
                    executor.execute(creditAgreementVerifierAgent, context);
                }

                case VALIDATED -> {
                    executor.execute(dealDataVerifierAgent, context);
                    executor.execute(creditAgreementVerifierAgent, context);
                }

                case DEAL_DATA_VERIFIED -> {
                    executor.execute(creditAgreementVerifierAgent, context);
                }

            }

        } catch (Exception e) {
            log.error("Error executing workflow: {}", workflow.getWorkflowId(), e);
        }

        return context;
    }


}