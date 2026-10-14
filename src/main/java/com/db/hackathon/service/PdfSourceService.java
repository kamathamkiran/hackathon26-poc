package com.db.hackathon.service;

import com.db.hackathon.dto.WorkflowContext;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PdfSourceService {

    private final Storage storage;

    public byte[] loadPdf(WorkflowContext context) {
        byte[] pdfBytes = context.getPdfBytes();
        if (pdfBytes != null && pdfBytes.length > 0) {
            return pdfBytes;
        }

        String bucketName = context.getSourceBucket();
        String filePath = context.getSourceFilePath();
        if (bucketName == null || bucketName.isBlank() || filePath == null || filePath.isBlank()) {
            throw new IllegalStateException("PDF source location is missing from the workflow context");
        }

        Blob blob = storage.get(bucketName, filePath);
        if (blob == null) {
            throw new IllegalStateException(
                    "PDF source was not found in Google Cloud Storage: gs://" + bucketName + "/" + filePath);
        }

        pdfBytes = blob.getContent();
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalStateException(
                    "PDF source is empty in Google Cloud Storage: gs://" + bucketName + "/" + filePath);
        }

        context.setPdfBytes(pdfBytes);
        return pdfBytes;
    }
}
