package com.db.hackathon.workflow;

import com.db.hackathon.dto.WorkflowContext;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.extraction.Agreement;
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

                context.setBucketName(node.get("bucket").asText());
                context.setFilePath(node.get("name").asText());

                log.info("Context: {}", context);
            }

            case PARSED -> {

                DocumentAnalysis analysis =
                        jsonSerializer.deserialize(
                                workflow.getMetadata(),
                                DocumentAnalysis.class);

                context.setDocumentAnalysis(analysis);
            }

            case EXTRACTED -> {

                Agreement agreement =
                        jsonSerializer.deserialize(
                                workflow.getMetadata(),
                                Agreement.class);

                context.setAgreement(agreement);
            }

            default -> {
            }
        }

        return context;
    }

}
