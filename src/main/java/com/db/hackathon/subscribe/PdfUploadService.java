package com.db.hackathon.subscribe;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PdfUploadService {
    private final Storage storage;
    @Value("${google.bucket.name}")
    private String bucketName;

    public String uploadPdf(String uuid, String username, MultipartFile file) throws IOException {
        if (file.isEmpty()) {
            throw new IllegalArgumentException("File is empty");
        }

        if (!file.getOriginalFilename().toLowerCase().endsWith(".pdf")) {
            throw new IllegalArgumentException("Only PDF files are allowed");
        }

        Map<String, String> metadata = new HashMap<>();
        metadata.put("uuid", uuid);
        metadata.put("username", username);

        BlobInfo blobInfo = BlobInfo.newBuilder(bucketName, file.getOriginalFilename())
                .setContentType("application/pdf")
                .setMetadata(metadata)
                .build();

        storage.create(blobInfo, file.getBytes());

        return String.format("gs://%s/%s", bucketName, file.getOriginalFilename());
    }
}
