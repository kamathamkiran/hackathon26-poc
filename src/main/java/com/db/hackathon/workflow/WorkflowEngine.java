package com.db.hackathon.workflow;

import com.db.hackathon.agent.WorkflowAgent;
import com.db.hackathon.registry.AgentRegistry;
import org.springframework.stereotype.Service;

@Service
public class WorkflowEngine {

    private final AgentRegistry registry;

    public WorkflowEngine(AgentRegistry registry) {
        this.registry = registry;
    }

    public WorkflowContext execute(WorkflowContext context) {

        context.setStatus(WorkflowStatus.RUNNING);

        while (context.getStatus() == WorkflowStatus.RUNNING) {

            WorkflowAgent agent =
                    registry.getAgent(context.getCurrentStep());

            context = agent.execute(context);

            if (context.getCurrentStep() == WorkflowStep.COMPLETED) {
                context.setStatus(WorkflowStatus.COMPLETED);
            }
        }

        return context;
    }
}
