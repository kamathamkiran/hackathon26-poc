package com.db.hackathon.agent;

import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.workflow.WorkflowStep;
import org.springframework.stereotype.Component;

@Component
public class DocumentAgent implements WorkflowAgent {

    @Override
    public WorkflowStep supports() {
        return WorkflowStep.PARSING;
    }

    @Override
    public WorkflowContext execute(WorkflowContext context) {

        // Parse PDF...

        // set the next step to extraction
        context.setCurrentStep(
                WorkflowStep.EXTRACTION
        );

        return context;
    }
}
