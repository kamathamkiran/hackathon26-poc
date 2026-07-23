package com.db.hackathon.controller;

import com.db.hackathon.service.WorkflowService;
import com.db.hackathon.subscribe.PdfUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
public class WorkflowController {

    private final WorkflowService workflowService;
    private final PdfUploadService pdfUploadService;

    @PostMapping(
            value = "/process",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public void process(
            @RequestParam("filePath") String filePath) {

        //return workflowService.process(filePath);
        return;
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(
            @RequestParam("uuid") String uuid,
            @RequestParam("username") String username,
            @RequestParam("file") MultipartFile file) {

        try {
            String fileUrl = pdfUploadService.uploadPdf(uuid, username, file);
            return ResponseEntity.ok("File uploaded successfully: " + fileUrl);

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Upload failed: " + e.getMessage());
        }
    }
}