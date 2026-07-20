package com.db.hackathon.agent;

import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.workflow.WorkflowStep;

public class ExtractionAgent implements WorkflowAgent {

    @Override
    public WorkflowStep supports() {
        return WorkflowStep.EXTRACTION;
    }

    @Override
    public WorkflowContext execute(WorkflowContext context) {

        // Later call ADK

        // set the next step to validation
        context.setCurrentStep(
                WorkflowStep.VALIDATION
        );

        return context;
    }
}
