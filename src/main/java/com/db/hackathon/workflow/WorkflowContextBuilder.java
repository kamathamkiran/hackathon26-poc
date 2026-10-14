package com.db.hackathon.workflow;

import com.db.hackathon.dto.ExtractionResponse;
import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.service.JsonSerializerService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WorkflowContextBuilder {

    private final JsonSerializerService jsonSerializer;

    public WorkflowContext build(WorkflowEntity workflow) {

        WorkflowContext context = WorkflowContext.builder()
                .workflow(workflow)
                .build();

        switch (workflow.getStatus()) {

            case UPLOADED -> {

                JsonNode node =
                        jsonSerializer.readTree(
                                workflow.getMetadata());

                context.setSourceBucket(node.get("bucket").asText());
                context.setSourceFilePath(node.get("name").asText());

                log.info("Context: {}", context);
            }

            case EXTRACTED, VALIDATED, REVIEWED -> {

                ExtractionResponse response =
                        jsonSerializer.deserialize(
                                workflow.getMetadata(),
                                ExtractionResponse.class);

                context.setDeal(response.getDeal());
                context.setReviewIssues(response.getReviewIssues());
                context.setOverallConfidence(response.getOverallConfidence());
                context.setSourceBucket(response.getSourceBucket());
                context.setSourceFilePath(response.getSourceFilePath());
            }

            default -> {
            }
        }

        return context;
    }

}
