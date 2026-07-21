package com.db.hackathon.dto;

import com.db.hackathon.workflow.WorkflowContext;
import com.db.hackathon.enums.WorkflowStatus;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WorkflowResponse {

    String workflowId;

    WorkflowStatus status;

    public static WorkflowResponse from(
            WorkflowContext context) {

        return WorkflowResponse.builder()

                .workflowId(context.getWorkflow().getWorkflowId())

                .status(context.getWorkflow().getStatus())

                .build();

    }

}
