package com.db.hackathon.workflow;

import com.db.hackathon.agents.HumanReviewAgent;
import com.db.hackathon.agents.WorkflowAgent;
import com.db.hackathon.agents.extraction.ExtractionAgent;
import com.db.hackathon.agents.review.ReviewAgent;
import com.db.hackathon.agents.validation.ValidationAgent;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.model.validation.ValidationIssue;
import com.db.hackathon.model.validation.ValidationResult;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WorkflowEngineTest {

    @Test
    void continuesToThirdReviewLoopAndStopsWhenValidationPasses() throws Exception {
        TestWorkflow workflow = new TestWorkflow();
        workflow.configureValidation(4);

        workflow.execute();

        InOrder order = inOrder(workflow.executor);
        order.verify(workflow.executor).execute(workflow.extractionAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.validationAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.reviewAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.validationAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.reviewAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.validationAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.reviewAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.validationAgent, workflow.context);
        order.verify(workflow.executor).execute(workflow.humanReviewAgent, workflow.context);
        verify(workflow.reviewAgent).refreshConfidence(workflow.context);
    }

    @Test
    void stopsAfterThreeReviewLoopsWhenValidationStillFails() throws Exception {
        TestWorkflow workflow = new TestWorkflow();
        workflow.configureValidation(Integer.MAX_VALUE);

        workflow.execute();

        verify(workflow.executor, org.mockito.Mockito.times(3))
                .execute(workflow.reviewAgent, workflow.context);
        verify(workflow.executor, org.mockito.Mockito.times(4))
                .execute(workflow.validationAgent, workflow.context);
        verify(workflow.executor).execute(workflow.humanReviewAgent, workflow.context);
    }

    private static class TestWorkflow {
        private final WorkflowContextBuilder contextBuilder = mock(WorkflowContextBuilder.class);
        private final WorkflowExecutor executor = mock(WorkflowExecutor.class);
        private final ExtractionAgent extractionAgent = mock(ExtractionAgent.class);
        private final ValidationAgent validationAgent = mock(ValidationAgent.class);
        private final ReviewAgent reviewAgent = mock(ReviewAgent.class);
        private final HumanReviewAgent humanReviewAgent = mock(HumanReviewAgent.class);
        private final WorkflowEntity workflow = WorkflowEntity.builder()
                .workflowId("workflow-1")
                .status(WorkflowStatus.EXTRACTING)
                .build();
        private final WorkflowContext context = WorkflowContext.builder()
                .workflow(workflow)
                .build();
        private int validOnRun;

        private void configureValidation(int validOnRun) throws Exception {
            this.validOnRun = validOnRun;
            when(contextBuilder.build(workflow)).thenReturn(context);
            AtomicInteger runs = new AtomicInteger();
            doAnswer(invocation -> {
                WorkflowAgent agent = invocation.getArgument(0);
                if (agent == validationAgent) {
                    boolean valid = runs.incrementAndGet() >= this.validOnRun;
                    context.setValidationResult(ValidationResult.builder()
                            .valid(valid)
                            .errors(valid ? List.of() : List.of(
                                    ValidationIssue.builder().field("dealName").build()))
                            .build());
                }
                return null;
            }).when(executor).execute(any(WorkflowAgent.class), same(context));
        }

        private void execute() {
            WorkflowEngine engine = new WorkflowEngine(contextBuilder, executor,
                    extractionAgent, validationAgent, reviewAgent, humanReviewAgent);
            engine.execute(workflow);
        }
    }
}
