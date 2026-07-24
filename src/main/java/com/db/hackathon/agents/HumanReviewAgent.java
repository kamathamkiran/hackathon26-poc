package com.db.hackathon.agents;

import com.db.hackathon.dto.ExtractionResponse;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.model.validation.ValidationIssue;
import com.db.hackathon.model.validation.ValidationResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HumanReviewAgent implements WorkflowAgent {

    @Override
    public AgentType getAgentType() {
        return AgentType.HUMAN_REVIEW;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.HUMAN_REVIEW_PENDING;
    }

    @Override
    public void process(WorkflowContext context) {
        log.info("Assembling final AI response for workflow {} and parking at HUMAN_REVIEW_PENDING",
                context.getWorkflow().getWorkflowId());
    }

    @Override
    public Object getOutput(WorkflowContext context) {

        ValidationResult validation = context.getValidationResult();
        List<ValidationIssue> issues = validation == null ? List.of() : validation.allIssues();

        return ExtractionResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .deal(context.getDeal())
                .validationIssues(issues)
                .reviewIssues(context.getReviewIssues() == null ? List.of() : context.getReviewIssues())
                .overallConfidence(context.getOverallConfidence())
                .build();
    }
}
