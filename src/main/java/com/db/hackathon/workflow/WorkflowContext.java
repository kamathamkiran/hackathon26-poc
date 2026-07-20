package com.db.hackathon.workflow;

import com.db.hackathon.model.Agreement;
import com.db.hackathon.model.ValidationResult;
import lombok.Data;

import java.util.*;

@Data
public class WorkflowContext {

    private UUID workflowId;

    private WorkflowStep currentStep;

    private WorkflowStatus status;

    private String pdfPath;

    private String rawText;

    private Agreement agreement;

    private ValidationResult validationResult;

    private Exception lastException;

    private Map<String, Object> metadata = new HashMap<>();

}
