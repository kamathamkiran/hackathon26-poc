package com.db.hackathon.registry;

import com.db.hackathon.agent.WorkflowAgent;
import com.db.hackathon.workflow.WorkflowStep;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AgentRegistry {

    private final Map<WorkflowStep, WorkflowAgent> registry;

    public AgentRegistry(List<WorkflowAgent> agents) {

        this.registry = agents.stream()
                .collect(Collectors.toMap(
                        WorkflowAgent::supports,
                        Function.identity()
                ));
    }

    public WorkflowAgent getAgent(WorkflowStep step) {

        WorkflowAgent agent = registry.get(step);

        if (agent == null) {
            throw new IllegalArgumentException(
                    "No agent registered for " + step
            );
        }

        return agent;
    }
}
