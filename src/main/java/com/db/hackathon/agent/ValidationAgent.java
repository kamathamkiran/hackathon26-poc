package com.db.hackathon.agent;

import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.workflow.WorkflowStep;

public class ValidationAgent implements WorkflowAgent {

    @Override
    public WorkflowStep supports() {
        return WorkflowStep.VALIDATION;
    }

    @Override
    public WorkflowContext execute(WorkflowContext context) {

        // Later call ADK

        // set the next step to completed
        context.setCurrentStep(
                WorkflowStep.COMPLETED
        );

        return context;
    }
}
