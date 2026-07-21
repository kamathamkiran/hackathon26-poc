package com.db.hackathon.dto;

import com.db.hackathon.model.aggrement.Agreement;
import lombok.Builder;
import lombok.Data;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.enums.WorkflowStatus;

@Data
@Builder
public class AgreementResponse {

    private String workflowId;

    private WorkflowStatus status;

    private Agreement agreement;

    private ValidationResult validationResult;

}
