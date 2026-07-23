package com.db.hackathon.dto;

import com.db.hackathon.model.extraction.Deal;
import lombok.Builder;
import lombok.Data;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.enums.WorkflowStatus;

@Data
@Builder
public class WorkflowResponse {

    private String workflowId;

    private WorkflowStatus status;

    private Deal deal;

    private ValidationResult validationResult;

}
