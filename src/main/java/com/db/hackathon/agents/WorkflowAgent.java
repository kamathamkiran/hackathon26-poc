package com.db.hackathon.agents;

import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.dto.WorkflowContext;

public interface WorkflowAgent {

    AgentType getAgentType();

    WorkflowStatus getOutputStatus();

    void process(WorkflowContext context) throws Exception;

    Object getOutput(WorkflowContext context);
}
