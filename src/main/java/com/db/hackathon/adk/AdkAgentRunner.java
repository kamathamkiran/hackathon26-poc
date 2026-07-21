package com.db.hackathon.adk;

import com.db.hackathon.workflow.WorkflowContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AdkAgentRunner {

    public WorkflowContext execute(
            WorkflowContext context){

        log.info("Calling Google ADK");

        return context;

    }

}
