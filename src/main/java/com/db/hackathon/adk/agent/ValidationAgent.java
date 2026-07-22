package com.db.hackathon.adk.agent;

import com.db.hackathon.model.extraction.Agreement;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.util.ValidationUtil;
import com.db.hackathon.workflow.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationAgent
        implements WorkflowAgent<WorkflowContext, WorkflowContext> {

    private final ValidationUtil validationUtil;

    @Override
    public WorkflowContext process(
            WorkflowContext context) {

        log.info("Starting validation");

        ValidationResult result = validate(context.getAgreement());

        context.setValidationResult(result);

        log.info("Validation completed");

        return context;

    }

    public ValidationResult validate(
            Agreement agreement) {

        ValidationResult result =
                ValidationResult.builder()
                        .valid(true)
                        .build();

        validationUtil.validateMandatoryFields(result, agreement);

        result.setValid(result.getErrors().isEmpty());

        return result;


    }
}
