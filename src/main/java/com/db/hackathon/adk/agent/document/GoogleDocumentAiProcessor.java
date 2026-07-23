package com.db.hackathon.adk.agent.document;

import com.db.hackathon.config.DocumentAiProperties;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDocumentAiProcessor {

    private static final String SUPPORTED_MIME_TYPE = "application/pdf";

    private final DocumentProcessorServiceClient client;
    private final DocumentAiProperties properties;
    private final DocumentAiMapper mapper;

    public DocumentAnalysis process(Path pdfPath) throws IOException {

        log.info("Processing document: path={}", pdfPath);

        RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent(ByteString.readFrom(java.nio.file.Files.newInputStream(pdfPath)))
                    .setMimeType(SUPPORTED_MIME_TYPE)
                    .build();

        String processorName = ProcessorName.of(
                    properties.getProjectId(),
                    properties.getLocation(),
                    properties.getProcessorId())
                    .toString();

        log.debug("Using processor: {}", processorName);

        ProcessRequest request = ProcessRequest.newBuilder()
                    .setName(processorName)
                    .setRawDocument(rawDocument)
                    .build();

        log.debug("Sending request to Document AI processor: {}", processorName);

        try {
            ProcessResponse response = client.processDocument(request);

            log.debug("Document processed successfully");
            return mapper.map(response.getDocument());
        } catch (Exception ex) {
            log.error("Failed to process document with processor: {}", processorName);
            throw ex;
        }
    }


}
