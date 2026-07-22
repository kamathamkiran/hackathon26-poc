package com.db.hackathon.adk.agent.document;

import com.db.hackathon.model.document.DocumentAnalysis;
import com.google.cloud.documentai.v1.*;
import com.google.protobuf.ByteString;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class GoogleDocumentAiProcessor {

    private static final String SUPPORTED_MIME_TYPE = "application/pdf";

    private final DocumentProcessorServiceClient client;
    private final DocumentAiProperties properties;
    private final DocumentAiMapper mapper;

    public DocumentAnalysis process(MultipartFile pdf) throws IOException {

        byte[] pdfBytes = pdf.getBytes();

        log.debug("Processing document: name={}, size={} bytes, mimeType={}",
                    pdf.getOriginalFilename(), pdfBytes.length, pdf.getContentType());

        RawDocument rawDocument = RawDocument.newBuilder()
                    .setContent( ByteString.copyFrom(pdfBytes))
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

        try {
            ProcessResponse response = client.processDocument(request);

            log.debug("Document processed successfully");
            return mapper.map(response.getDocument());
        } catch (Exception ex) {
            log.error("Failed to process document with processor: {}, file: {}, size: {} bytes",
                            processorName, pdf.getOriginalFilename(), pdfBytes.length, ex);
            throw ex;
        }
    }


}
