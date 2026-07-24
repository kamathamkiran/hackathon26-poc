package com.db.hackathon.adk.agent.validation;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.dto.WorkflowResponse;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.dto.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationAgent
        implements WorkflowAgent {

    private final ValidationService validationUtil;

    @Override
    public AgentType getAgentType() {
        return AgentType.VALIDATION;
    }

    @Override
    public WorkflowStatus getInputStatus() {
        return WorkflowStatus.VALIDATING;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.VALIDATED;
    }

    @Override
    public void process(
            WorkflowContext context) throws Exception {

        String workflowId = context.getWorkflow().getWorkflowId();
        log.info("Starting validation for workflowId={}", workflowId);

        ValidationResult result = validate(context.getDeal());

        context.setValidationResult(result);

        int errorCount = result.getErrors() == null ? 0 : result.getErrors().size();
        log.info("Validation completed for workflowId={}: {} error(s)", workflowId, errorCount);

    }

    @Override
    public Object getOutput(WorkflowContext context) {

        return WorkflowResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .status(context.getWorkflow().getStatus())
                .deal(context.getDeal())
                .validationResult(context.getValidationResult())
                .build();
    }

    public ValidationResult validate(
            Deal deal) {

        ValidationResult result =
                ValidationResult.builder()
                        .valid(true)
                        .build();

//        validationUtil.validateMandatoryFields(deal);

        result.setValid(result.getErrors()==null || result.getErrors().isEmpty());

        return result;


    }
}
