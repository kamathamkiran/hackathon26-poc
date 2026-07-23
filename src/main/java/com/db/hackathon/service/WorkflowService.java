package com.db.hackathon.service;

import com.db.hackathon.dto.WorkflowResponse;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.repository.WorkflowRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
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

    public WorkflowResponse process(JsonNode fileMetadata) {
        String uuid = fileMetadata.get("uuid").asText();
        WorkflowEntity workflow = WorkflowEntity.builder()
                .workflowId(UUID.randomUUID().toString()) //pass uuid available in metadata
                .username("OPS_USER")
                .status(WorkflowStatus.UPLOADED)
                .nextAgent(AgentType.DOCUMENT_PARSER)
                .metadata(fileMetadata.toString())
                .build();

        workflowRepository.save(workflow);

        WorkflowContext context =
                workflowEngine.execute(workflow);

        return WorkflowResponse.builder()
                .workflowId(context.getWorkflow().getWorkflowId())
                .status(context.getWorkflow().getStatus())
                .deal(context.getDeal())
                .validationResult(context.getValidationResult())
                .build();

    }

}
