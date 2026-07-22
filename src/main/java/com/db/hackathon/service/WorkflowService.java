package com.db.hackathon.service;

import com.db.hackathon.dto.WorkflowResponse;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.workflow.WorkflowEngine;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowEngine workflowEngine;
    private final WorkflowRepository workflowRepository;
    private final JsonSerializerService jsonSerializer;

    public WorkflowResponse process(String filePath) {

        WorkflowEntity workflow = WorkflowEntity.builder()
                .workflowId(UUID.randomUUID().toString())
                .username("OPS_USER")
                .fileName("sample.pdf")
                .status(WorkflowStatus.UPLOADED)
                .lastCompletedAgent(AgentType.DOCUMENT_PARSER)
                .jsonOutput(
                        jsonSerializer.serialize(
                        Map.of("filePath", filePath)
                ))
                .build();

        workflowRepository.save(workflow);

        WorkflowContext context =
                workflowEngine.execute(workflow);

        return WorkflowResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .status(context.getWorkflow().getStatus())
                .agreement(context.getAgreement())
                .validationResult(context.getValidationResult())
                .build();

    }

}
