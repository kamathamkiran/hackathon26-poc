package com.db.hackathon.adk.agent.verification;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.exception.WorkflowVerificationException;
import com.db.hackathon.model.verification.DealDataVerificationResult;
import com.db.hackathon.service.JsonSerializerService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Workflow adapter for the deterministic schema and business-rule gate. */
@Component
@RequiredArgsConstructor
@Slf4j
public class DealDataVerifierAgent implements WorkflowAgent {

    private final DealDataVerifier verifier;
    private final JsonSerializerService jsonSerializer;

    @Override
    public AgentType getAgentType() {
        return AgentType.DEAL_DATA_VERIFIER;
    }

    @Override
    public WorkflowStatus getInputStatus() {
        return WorkflowStatus.VALIDATED;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.DEAL_DATA_VERIFIED;
    }

    @Override
    public void process(WorkflowContext context) {
        String json = context.getDealDataJson();
        if (json == null || json.isBlank()) {
            if (context.getDeal() == null) {
                throw new WorkflowVerificationException("No extracted deal data was available for verification");
            }
            json = jsonSerializer.serialize(context.getDeal());
            context.setDealDataJson(json);
        }

        DealDataVerificationResult result = verifier.verify(json);
        context.setDealDataVerificationResult(result);

        if (!result.isValid()) {
            log.warn("Deal data verification failed: {}", result.getErrors());
            throw new WorkflowVerificationException("Deal data verification failed: "
                    + String.join("; ", result.getErrors()));
        }

        log.info("Deal data verification passed for workflow {}",
                context.getWorkflow().getWorkflowId());
    }

    @Override
    public Object getOutput(WorkflowContext context) {
        return context.getDealDataVerificationResult();
    }
}
