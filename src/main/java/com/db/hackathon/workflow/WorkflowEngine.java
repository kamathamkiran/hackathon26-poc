package com.db.hackathon.workflow;

import com.db.hackathon.agents.extraction.ExtractionAgent;
import com.db.hackathon.agents.HumanReviewAgent;
import com.db.hackathon.agents.review.ReviewAgent;
import com.db.hackathon.agents.validation.ValidationAgent;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.entity.WorkflowEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class WorkflowEngine {

    private static final int MAX_REVIEW_LOOPS = 3;

    private final WorkflowContextBuilder contextBuilder;
    private final WorkflowExecutor executor;

    private final ExtractionAgent extractionAgent;
    private final ValidationAgent validationAgent;
    private final ReviewAgent reviewAgent;
    private final HumanReviewAgent humanReviewAgent;

    public void execute(WorkflowEntity workflow) {

        WorkflowContext context = contextBuilder.build(workflow);

        try {
            log.info("Executing workflow: {}", workflow.getWorkflowId());

            switch (workflow.getStatus()) {

                case EXTRACTING -> {
                    executor.execute(extractionAgent, context);
                    validateAndReview(context);
                }

                case VALIDATING, REVIEWING -> validateAndReview(context);

                case UPLOADING, HUMAN_REVIEW_PENDING, COMPLETED, FAILED -> {
                    // These states are not resumable processing stages.
                }

            }

        } catch (Exception e) {
            log.error("Error executing workflow: {}", workflow.getWorkflowId(), e);
        }

    }

    private void validateAndReview(WorkflowContext context) throws Exception {
        executor.execute(validationAgent, context);

        for (int loop = 1; loop <= MAX_REVIEW_LOOPS; loop++) {
            executor.execute(reviewAgent, context);
            executor.execute(validationAgent, context);

            if (context.getValidationResult() != null
                    && context.getValidationResult().isValid()) {
                log.info("Validation passed after review loop {} for workflow {}",
                        loop, context.getWorkflow().getWorkflowId());
                break;
            }

            if (loop == MAX_REVIEW_LOOPS) {
                log.warn("Workflow {} still has validation errors after {} review loops",
                        context.getWorkflow().getWorkflowId(), MAX_REVIEW_LOOPS);
            }
        }

        reviewAgent.refreshConfidence(context);
        executor.execute(humanReviewAgent, context);
    }


}