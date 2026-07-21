package com.db.hackathon.workflow;

import com.db.hackathon.adk.AdkAgentRunner;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final AdkAgentRunner runner;

    public WorkflowContext start(
            WorkflowContext context){

        log.info("Workflow Started {}", context.getWorkflowId());

        context.setStatus(WorkflowStatus.RUNNING);

        return runner.execute(context);

    }

}
