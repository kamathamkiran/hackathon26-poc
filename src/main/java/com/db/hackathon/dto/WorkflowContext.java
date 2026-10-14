package com.db.hackathon.dto;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.model.review.ReviewIssue;
import com.db.hackathon.model.validation.ValidationResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class WorkflowContext {

    private WorkflowEntity workflow;

    private String sourceBucket;

    private String sourceFilePath;

    private byte[] pdfBytes;

    private Deal deal;

    private ValidationResult validationResult;

    private List<ReviewIssue> reviewIssues;

    private Double overallConfidence;

    private String dealDataJson;
}
