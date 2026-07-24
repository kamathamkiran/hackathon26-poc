package com.db.hackathon.dto;

import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.model.review.ReviewIssue;
import com.db.hackathon.model.validation.ValidationIssue;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExtractionResponse {

    private String workflowId;

    private Deal deal;

    private List<ValidationIssue> validationIssues;

    private List<ReviewIssue> reviewIssues;

    private Double overallConfidence;
}
