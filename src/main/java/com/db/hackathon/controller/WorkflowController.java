package com.db.hackathon.controller;

import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.workflow.WorkflowEngine;
import com.db.hackathon.workflow.WorkflowStatus;
import com.db.hackathon.workflow.WorkflowStep;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowEngine workflowEngine;

    @PostMapping("/start")
    public WorkflowContext startWorkflow() {

        WorkflowContext context = new WorkflowContext();

        context.setWorkflowId(UUID.randomUUID());

        context.setStatus(WorkflowStatus.CREATED);

        context.setCurrentStep(WorkflowStep.PARSING);

        return workflowEngine.start(context);

    }

}