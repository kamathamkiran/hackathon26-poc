package com.db.hackathon.dto;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.extraction.Agreement;
import com.db.hackathon.model.validation.ValidationResult;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class WorkflowContext {

    private WorkflowEntity workflow;

    private String filePath;

    private DocumentAnalysis documentAnalysis;

    private Agreement agreement;

    private ValidationResult validationResult;
}
