package com.db.hackathon.workflow;

import com.db.hackathon.agents.document.DocumentParserAgent;
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

    private final WorkflowContextBuilder contextBuilder;
    private final WorkflowExecutor executor;

    private final DocumentParserAgent parserAgent;
    private final ExtractionAgent extractionAgent;
    private final ValidationAgent validationAgent;
    private final ReviewAgent reviewAgent;
    private final HumanReviewAgent humanReviewAgent;

    public WorkflowContext execute(WorkflowEntity workflow) {

        WorkflowContext context = contextBuilder.build(workflow);

        try {
            log.info("Executing workflow: {}", workflow.getWorkflowId());

            switch (workflow.getStatus()) {

                case UPLOADED -> {
                    executor.execute(parserAgent, context);
                    executor.execute(extractionAgent, context);
                    executor.execute(validationAgent, context);
                    executor.execute(reviewAgent, context);
                    executor.execute(humanReviewAgent, context);
                }

                case PARSED -> {
                    executor.execute(extractionAgent, context);
                    executor.execute(validationAgent, context);
                    executor.execute(reviewAgent, context);
                    executor.execute(humanReviewAgent, context);
                }

                case EXTRACTED -> {
                    executor.execute(validationAgent, context);
                    executor.execute(reviewAgent, context);
                    executor.execute(humanReviewAgent, context);
                }

                case VALIDATED -> {
                    executor.execute(reviewAgent, context);
                    executor.execute(humanReviewAgent, context);
                }

                case REVIEWED -> executor.execute(humanReviewAgent, context);

            }

        } catch (Exception e) {
            log.error("Error executing workflow: {}", workflow.getWorkflowId(), e);
        }

        return context;
    }


}