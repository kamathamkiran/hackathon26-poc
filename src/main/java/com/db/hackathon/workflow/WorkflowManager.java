package com.db.hackathon.workflow;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.repository.WorkflowEventRepository;
import com.db.hackathon.repository.WorkflowRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class WorkflowManager {

    private final WorkflowRepository workflowRepository;

    private final WorkflowEventRepository eventRepository;

    public WorkflowEntity createWorkflow(
            MultipartFile pdf) {

        WorkflowEntity workflow =
                WorkflowEntity.builder()
                        .workflowId(UUID.randomUUID().toString())
                        .fileName(pdf.getOriginalFilename())
                        .status(WorkflowStatus.UPLOADED)
                        .createdAt(Instant.now())
                        .build();

        workflowRepository.save(workflow);

        addEvent(workflow,
                WorkflowStatus.UPLOADED,
                "Workflow Created");

        return workflow;

    }

    public void updateStatus(
            WorkflowEntity workflow,
            WorkflowStatus status,
            String remarks) {

        workflow.setStatus(status);
        workflow.setCompletedAt(Instant.now());

        workflowRepository.save(workflow);

        addEvent(workflow,
                status,
                remarks);

    }

    public void markFailed(
            WorkflowEntity workflow,
            Exception exception) {

        workflow.setFailureReason(exception.getMessage());

        updateStatus(
                workflow,
                WorkflowStatus.FAILED,
                exception.getMessage());

    }

    private void addEvent(
            WorkflowEntity workflow,
            WorkflowStatus status,
            String remarks) {

        WorkflowEventEntity event =
                WorkflowEventEntity.builder()
                        .workflow(workflow)
                        .status(status)
                        .remarks(remarks)
                        .createdAt(Instant.now())
                        .build();

        eventRepository.save(event);

    }

}
