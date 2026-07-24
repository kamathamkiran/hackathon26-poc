package com.db.hackathon.agents.validation;

import com.db.hackathon.agents.WorkflowAgent;
import com.db.hackathon.dto.ExtractionResponse;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.model.validation.ValidationIssue;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.dto.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ValidationAgent
        implements WorkflowAgent {

    private final ValidationService validationService;

    @Override
    public AgentType getAgentType() {
        return AgentType.VALIDATION;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.VALIDATED;
    }

    @Override
    public void process(
            WorkflowContext context) throws Exception {

        log.info("Starting validation");

        ValidationResult result = validationService.validate(context.getDeal());

        context.setValidationResult(result);

        log.info("Validation completed: {} error(s), {} warning(s)",
                result.getErrors() == null ? 0 : result.getErrors().size(),
                result.getWarnings() == null ? 0 : result.getWarnings().size());
    }

    @Override
    public Object getOutput(WorkflowContext context) {

        ValidationResult validation = context.getValidationResult();
        List<ValidationIssue> issues = validation == null ? List.of() : validation.allIssues();

        return ExtractionResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .deal(context.getDeal())
                .validationIssues(issues)
                .build();
    }
}
