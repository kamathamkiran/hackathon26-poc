package com.db.hackathon.agent;

import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.workflow.WorkflowStep;

public interface WorkflowAgent {

    WorkflowStep supports();

    WorkflowContext execute(WorkflowContext context);

}
