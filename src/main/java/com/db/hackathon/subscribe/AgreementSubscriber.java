package com.db.hackathon.subscribe;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.repository.WorkflowRepository;
import com.db.hackathon.service.WorkflowService;
import com.db.hackathon.workflow.WorkflowEngine;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.support.BasicAcknowledgeablePubsubMessage;
import com.google.cloud.spring.pubsub.support.GcpPubSubHeaders;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class AgreementSubscriber {
    private final WorkflowRepository workflowRepository;
    private final WorkflowEngine workflowEngine;
    private final ObjectMapper mapper;

    @ServiceActivator(inputChannel = "agreementInputChannel")
    public void receive(Message<String> message) {
        BasicAcknowledgeablePubsubMessage originalMessage =
                message.getHeaders().get(
                        GcpPubSubHeaders.ORIGINAL_MESSAGE,
                        BasicAcknowledgeablePubsubMessage.class);

        try {
            log.info("Received Pub/Sub payload : {}", message.getPayload());

            JsonNode fileMetadata = mapper.readTree(message.getPayload());
            originalMessage.ack();

            // Read custom metadata
            JsonNode metadata = fileMetadata.path("metadata");

            String uuid = metadata.path("uuid").asText();
            String username = metadata.path("username").asText();

            WorkflowEntity workflow = WorkflowEntity.builder()
                    .workflowId(uuid) //pass uuid available in metadata
                    .username(username)
                    .status(WorkflowStatus.UPLOADED)
                    .nextAgent(AgentType.DOCUMENT_PARSER)
                    .metadata(fileMetadata.toString())
                    .build();

            workflowRepository.save(workflow);

            log.info("Saved data into database");

            log.info("Triggering Orchestrator");

            workflowEngine.execute(workflow);

        } catch (Exception ex) {
            log.error("Failed to process Pub/Sub message", ex);
            assert originalMessage != null;
            originalMessage.ack();
        }
    }
}
