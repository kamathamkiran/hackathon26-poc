package com.db.hackathon.workflow;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowExecutor {

    private final WorkflowManager workflowManager;

    public void execute(
            WorkflowAgent agent,
            WorkflowContext context) throws Exception {

        WorkflowEntity workflow = context.getWorkflow();

        WorkflowEventEntity event = workflowManager.getOrCreateEvent(
                workflow.getWorkflowId(),
                agent.getAgentType());

        long startTime = System.currentTimeMillis();

        try {

            log.info("Executing {} for workflow {}",
                    agent.getAgentType(),
                    workflow.getWorkflowId());

            agent.process(context);

            workflowManager.saveCheckpoint(
                    workflow,
                    agent.getOutputStatus(),
                    agent.getAgentType(),
                    agent.getOutput(context));

            boolean trackEvent = agent.getAgentType() != AgentType.HUMAN_REVIEW;

            if(trackEvent) {
                workflowManager.completeEvent(
                        event,
                        System.currentTimeMillis() - startTime);
            }

            log.info("{} completed successfully for workflow {}",
                    agent.getAgentType(),
                    workflow.getWorkflowId());

            if (workflow.getStatus() == WorkflowStatus.HUMAN_REVIEW_PENDING) {
                log.info("Workflow {} is awaiting human review.", workflow.getWorkflowId());
            }

        } catch (Exception ex) {

            log.error("{} failed for workflow {}",
                    agent.getAgentType(),
                    workflow.getWorkflowId(),
                    ex);

            handleFailure(workflow, event, ex);

            throw ex;
        }
    }

    private void handleFailure(
            WorkflowEntity workflow,
            WorkflowEventEntity event,
            Exception ex) {

        boolean retry =
                workflowManager.incrementRetry(event, ex.getMessage());

        if (!retry) {

            workflowManager.markFailed(
                    workflow,
                    ex.getMessage());


            log.error(
                    "Workflow {} marked FAILED after maximum retries.",
                    workflow.getWorkflowId());
        }
    }

}
