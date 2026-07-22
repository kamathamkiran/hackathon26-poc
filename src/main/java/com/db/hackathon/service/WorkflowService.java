package com.db.hackathon.service;

import com.db.hackathon.dto.WorkflowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.workflow.WorkflowEngine;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowEngine workflowEngine;

    public WorkflowResponse process(MultipartFile file) {

        WorkflowContext context =
                workflowEngine.execute(file);

        return WorkflowResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .status(context.getWorkflow().getStatus())
                .agreement(context.getAgreement())
                .validationResult(context.getValidationResult())
                .build();

    }

}
