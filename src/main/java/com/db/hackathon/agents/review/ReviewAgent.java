package com.db.hackathon.agents.review;

import com.db.hackathon.agents.WorkflowAgent;
import com.db.hackathon.agents.extraction.SemanticRunnerService;
import com.db.hackathon.dto.ExtractionResponse;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.model.review.ReviewIssue;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.service.JsonSerializerService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ReviewAgent implements WorkflowAgent {

    private final SemanticRunnerService runnerService;
    private final JsonSerializerService jsonSerializer;
    private final ReviewAnalyzer reviewAnalyzer;
    private final DealNormalizer dealNormalizer;

    @Override
    public AgentType getAgentType() {
        return AgentType.REVIEW;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.REVIEWED;
    }

    @Override
    public void process(WorkflowContext context) throws Exception {

        log.info("Starting extraction review");

        Deal deal = context.getDeal();
        if (deal == null) {
            log.warn("No extracted deal available; skipping review");
            context.setReviewIssues(List.of());
            context.setOverallConfidence(0.0);
            return;
        }

        String originalJson = jsonSerializer.serialize(deal);

        if (context.getDocumentAnalysis() == null) {
            log.warn("No document analysis available; keeping extracted deal without review");
            context.setDeal(dealNormalizer.normalize(deal));
            context.setReviewIssues(List.of());
            context.setOverallConfidence(
                    reviewAnalyzer.overallConfidence(
                            jsonSerializer.readTree(originalJson), errorCount(context)));
            return;
        }

        Deal reviewed = runnerService.reviewAgreement(
                context.getDocumentAnalysis(), originalJson, context.getValidationResult());
        reviewed = dealNormalizer.normalize(reviewed);
        context.setDeal(reviewed);

        JsonNode originalTree = jsonSerializer.readTree(originalJson);
        JsonNode reviewedTree = jsonSerializer.readTree(jsonSerializer.serialize(reviewed));

        List<ReviewIssue> reviewIssues = reviewAnalyzer.diff(originalTree, reviewedTree);
        double overallConfidence = reviewAnalyzer.overallConfidence(reviewedTree, errorCount(context));

        context.setReviewIssues(reviewIssues);
        context.setOverallConfidence(overallConfidence);

        log.info("Extraction review completed: {} field change(s), overall confidence {}",
                reviewIssues.size(), overallConfidence);
    }

    private int errorCount(WorkflowContext context) {
        ValidationResult validation = context.getValidationResult();
        return validation == null || validation.getErrors() == null
                ? 0 : validation.getErrors().size();
    }

    @Override
    public Object getOutput(WorkflowContext context) {

        ValidationResult validation = context.getValidationResult();

        return ExtractionResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .deal(context.getDeal())
                .validationIssues(validation == null ? List.of() : validation.allIssues())
                .reviewIssues(context.getReviewIssues() == null ? List.of() : context.getReviewIssues())
                .overallConfidence(context.getOverallConfidence())
                .build();
    }
}
