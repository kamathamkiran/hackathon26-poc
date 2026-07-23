package com.db.hackathon.adk.agent.document;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.dto.WorkflowContext;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParserAgent implements WorkflowAgent {
    private final Storage storage;
    private final GoogleDocumentAiProcessor processor;

    @Override
    public AgentType getAgentType() {
        return AgentType.DOCUMENT_PARSER;
    }

    @Override
    public WorkflowStatus getInputStatus() {
        return WorkflowStatus.PARSING;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.PARSED;
    }

    @Override
    public Object getOutput(WorkflowContext context) {
        return context.getDocumentAnalysis();
    }

    @Override
    public void process(
            WorkflowContext context) throws Exception {

        String bucketName = context.getBucketName();
        String objectName = context.getFilePath();

        Blob blob = storage.get(bucketName, objectName);
        byte[] pdfBytes = blob.getContent();

        DocumentAnalysis analysis =
                processor.process(pdfBytes,Path.of(context.getFilePath()));

        context.setDocumentAnalysis(analysis);

    }

}