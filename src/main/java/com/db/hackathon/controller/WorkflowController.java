package com.db.hackathon.controller;

import com.db.hackathon.subscribe.PdfUploadService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
@Slf4j
public class WorkflowController {

    private final PdfUploadService pdfUploadService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(
            @RequestParam("uuid") String uuid,
            @RequestParam("username") String username,
            @RequestParam("file") MultipartFile file) {

        try {
            log.info("Input received {} {} {} ", uuid, username, file.getOriginalFilename());
            String fileUrl = pdfUploadService.uploadPdf(uuid, username, file);
            return ResponseEntity.ok("File uploaded successfully: " + fileUrl);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Upload failed: " + e.getMessage());
        }
    }
}