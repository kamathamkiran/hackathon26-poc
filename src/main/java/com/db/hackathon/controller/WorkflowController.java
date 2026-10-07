package com.db.hackathon.controller;

import com.db.hackathon.subscribe.PdfUploadService;
import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.EventStatus;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.repository.WorkflowEventRepository;
import com.db.hackathon.repository.WorkflowRepository;
import com.db.hackathon.service.JsonSerializerService;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/workflow")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = { "http://localhost:3000", "http://localhost:5173" })
public class WorkflowController {

    private final PdfUploadService pdfUploadService;
    private final WorkflowRepository workflowRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final JsonSerializerService jsonSerializer;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadPdf(
            @RequestParam("uuid") String uuid,
            @RequestParam("username") String username,
            @RequestParam("file") MultipartFile file) {

        LocalDateTime startedAt = LocalDateTime.now();
        WorkflowEventEntity uploadEvent = WorkflowEventEntity.builder()
                .id(UUID.randomUUID().toString())
                .workflowId(uuid)
                .agent(AgentType.UPLOAD)
                .status(EventStatus.STARTED)
                .retryCount(0)
                .durationMs(0L)
                .build();
        workflowEventRepository.save(uploadEvent);

        try {
            log.info("Input received {} {} {} ", uuid, username, file.getOriginalFilename());
            String fileUrl = pdfUploadService.uploadPdf(uuid, username, file);
            uploadEvent.setStatus(EventStatus.SUCCESS);
            uploadEvent.setDurationMs(Duration.between(startedAt, LocalDateTime.now()).toMillis());
            workflowEventRepository.save(uploadEvent);
            return ResponseEntity.ok("File uploaded successfully: " + fileUrl);

        } catch (Exception e) {
            uploadEvent.setStatus(EventStatus.FAILED);
            uploadEvent.setFailureReason(e.getMessage());
            uploadEvent.setDurationMs(Duration.between(startedAt, LocalDateTime.now()).toMillis());
            workflowEventRepository.save(uploadEvent);
            return ResponseEntity.badRequest()
                    .body("Upload failed: " + e.getMessage());
        }
    }

    @GetMapping("/{uuid}/status")
    public ResponseEntity<?> getWorkflowStatus(@PathVariable String uuid) {
        WorkflowEntity workflow = workflowRepository.findById(uuid).orElse(null);
        WorkflowEventEntity latestEvent = workflowEventRepository
                .findTopByWorkflowIdOrderByUpdatedAtDesc(uuid)
                .orElse(null);
        if (workflow == null && latestEvent == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(statusPayload(uuid, workflow, latestEvent));
    }

    @GetMapping("/{uuid}/metadata")
    public ResponseEntity<?> getWorkflowMetadata(@PathVariable String uuid) {
        return workflowRepository.findById(uuid)
                .<ResponseEntity<?>>map(workflow -> {
                    if (workflow.getStatus() != WorkflowStatus.HUMAN_REVIEW_PENDING
                            && workflow.getStatus() != WorkflowStatus.HUMAN_REVIEW_COMPLETED
                            && workflow.getStatus() != WorkflowStatus.DEAL_CREATED) {
                        return ResponseEntity.status(HttpStatus.CONFLICT)
                                .body(Map.of("message", "Workflow output is not ready", "status",
                                        workflow.getStatus().name()));
                    }
                    if (workflow.getMetadata() == null || workflow.getMetadata().isBlank()) {
                        return ResponseEntity.noContent().build();
                    }
                    return ResponseEntity.ok(jsonSerializer.readTree(workflow.getMetadata()));
                })
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{uuid}/sign-off")
    @Transactional
    public ResponseEntity<?> signOffWorkflow(
            @PathVariable String uuid,
            @RequestBody JsonNode reviewOutput) {
        WorkflowEntity workflow = workflowRepository.findById(uuid).orElse(null);
        if (workflow == null) {
            return ResponseEntity.notFound().build();
        }
        if (workflow.getStatus() == WorkflowStatus.HUMAN_REVIEW_COMPLETED) {
            return ResponseEntity.ok(signOffPayload(workflow));
        }
        if (workflow.getStatus() != WorkflowStatus.HUMAN_REVIEW_PENDING) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Workflow is not awaiting human sign-off", "status",
                            workflow.getStatus().name()));
        }

        JsonNode reviewedFields = reviewOutput.path("reviewedFields");
        if (!reviewedFields.isArray() || reviewedFields.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("message", "At least one approved review field is required"));
        }
        for (JsonNode field : reviewedFields) {
            if (!"APPROVED".equals(field.path("status").asText())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("message", "All review fields must be approved before sign-off"));
            }
        }

        WorkflowEventEntity reviewEvent = workflowEventRepository
                .findTopByWorkflowIdAndAgentOrderByUpdatedAtDesc(uuid, AgentType.HUMAN_REVIEW)
                .orElse(null);
        if (reviewEvent == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("message", "Human review event was not found"));
        }

        LocalDateTime completedAt = LocalDateTime.now();
        reviewEvent.setStatus(EventStatus.SUCCESS);
        reviewEvent.setDurationMs(Math.max(0, Duration.between(reviewEvent.getCreatedAt(), completedAt).toMillis()));
        reviewEvent.setFailureReason(null);

        workflow.setHumanJson(jsonSerializer.serialize(reviewOutput));
        workflow.setStatus(WorkflowStatus.HUMAN_REVIEW_COMPLETED);
        workflow.setNextAgent(null);
        workflow.setCompletedAt(completedAt);

        workflowEventRepository.save(reviewEvent);
        workflowRepository.save(workflow);

        return ResponseEntity.ok(signOffPayload(workflow));
    }

    private Map<String, String> signOffPayload(WorkflowEntity workflow) {
        return Map.of(
                "workflowId", workflow.getWorkflowId(),
                "status", workflow.getStatus().name(),
                "completedAt", workflow.getCompletedAt() == null ? "" : workflow.getCompletedAt().toString());
    }

    private Map<String, Object> statusPayload(String uuid, WorkflowEntity workflow, WorkflowEventEntity latestEvent) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("uuid", uuid);
        payload.put("status", workflow == null
                ? (latestEvent.getStatus() == EventStatus.FAILED ? "FAILED"
                        : latestEvent.getStatus() == EventStatus.SUCCESS ? "UPLOADED" : "UPLOADING")
                : workflow.getStatus().name());
        payload.put("nextAgent",
                workflow == null || workflow.getNextAgent() == null ? null : workflow.getNextAgent().name());
        payload.put("username", workflow == null ? null : workflow.getUsername());
        payload.put("updatedAt", workflow != null && workflow.getUpdatedAt() != null
                ? workflow.getUpdatedAt().toString()
                : latestEvent == null ? null : latestEvent.getUpdatedAt().toString());
        payload.put("completedAt", workflow == null || workflow.getCompletedAt() == null
                ? null
                : workflow.getCompletedAt().toString());
        payload.put("failureReason", workflow == null ? null : workflow.getFailureReason());
        payload.put("currentAgent", latestEvent == null ? null : latestEvent.getAgent().name());
        payload.put("eventStatus", latestEvent == null ? null : latestEvent.getStatus().name());
        payload.put("failedAgent", latestEvent != null && latestEvent.getStatus() == EventStatus.FAILED
                ? latestEvent.getAgent().name()
                : null);
        payload.put("eventFailureReason", latestEvent != null && latestEvent.getStatus() == EventStatus.FAILED
                ? latestEvent.getFailureReason()
                : null);
        return payload;
    }
}