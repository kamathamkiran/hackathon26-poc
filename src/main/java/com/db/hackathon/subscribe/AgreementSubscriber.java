package com.db.hackathon.subscribe;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.service.WorkflowService;
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
    private final WorkflowService workflowService;
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
            String uuid = fileMetadata.get("uuid").asText();
            WorkflowEntity workflow = WorkflowEntity.builder()
                    .workflowId(UUID.randomUUID().toString()) //pass uuid available in metadata
                    .username("OPS_USER")
                    .status(WorkflowStatus.UPLOADED)
                    .nextAgent(AgentType.DOCUMENT_PARSER)
                    .metadata(fileMetadata.toString())
                    .build();

        } catch (Exception ex) {
            log.error("Failed to process Pub/Sub message", ex);
            originalMessage.ack();
        }
    }
}
