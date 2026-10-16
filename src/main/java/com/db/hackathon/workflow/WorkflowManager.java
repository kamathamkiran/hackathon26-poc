package com.db.hackathon.workflow;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.EventStatus;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.dto.ExtractionResponse;
import com.db.hackathon.repository.WorkflowEventRepository;
import com.db.hackathon.repository.WorkflowRepository;
import com.db.hackathon.service.JsonSerializerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static java.time.ZoneId.systemDefault;

@Service
@RequiredArgsConstructor
public class WorkflowManager {

    private static final int MAX_RETRY = 3;

    private final WorkflowRepository workflowRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final JsonSerializerService jsonSerializer;

    public void saveWorkflowStatus(WorkflowEntity workflow) {
        workflowRepository.save(workflow);
    }

    public void saveCheckpoint(
            WorkflowEntity workflow,
            WorkflowStatus status,
            Object output,
            String sourceBucket,
            String sourceFilePath) {

        workflow.setStatus(status);
        if (output instanceof ExtractionResponse response) {
            response.setSourceBucket(sourceBucket);
            response.setSourceFilePath(sourceFilePath);
        }
        workflow.setMetadata(jsonSerializer.serialize(output));
        workflowRepository.save(workflow);
    }

    public void markFailed(
            WorkflowEntity workflow,
            String reason) {

        workflow.setStatus(WorkflowStatus.FAILED);
        workflow.setFailureReason(reason);

        workflowRepository.save(workflow);
    }

    @Transactional
    public boolean incrementRetry(
            WorkflowEventEntity event,
            String reason,
            WorkflowEntity workflow,
            long tokenUsage) {

        long durationMs = System.currentTimeMillis() - event.getCreatedAt().atZone(systemDefault()).toInstant().toEpochMilli();

        event.setStatus(EventStatus.FAILED);
        event.setDurationMs(durationMs);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setFailureReason(reason);
        addTokenUsage(event, tokenUsage);

        workflowEventRepository.save(event);
        updateWorkflowTokenUsage(workflow);

        return event.getRetryCount() < MAX_RETRY;
    }

    @Transactional
    public void completeEvent(
            WorkflowEventEntity event,
            long durationMs,
            WorkflowEntity workflow,
            long tokenUsage) {

        event.setStatus(EventStatus.SUCCESS);
        event.setDurationMs(durationMs);
        addTokenUsage(event, tokenUsage);

        workflowEventRepository.save(event);
        updateWorkflowTokenUsage(workflow);
    }

    public WorkflowEventEntity getOrCreateEvent(
            String workflowId,
            AgentType agent) {

        return workflowEventRepository
                .findTopByWorkflowIdAndAgentOrderByUpdatedAtDesc(
                        workflowId,
                        agent)
                .orElseGet(() ->
                        workflowEventRepository.save(
                                WorkflowEventEntity.builder()
                                        .id(agent.getWorkflowEventId())
                                        .workflowId(workflowId)
                                        .agent(agent)
                                        .status(EventStatus.STARTED)
                                        .retryCount(0)
                                        .build()));
    }

    private void addTokenUsage(WorkflowEventEntity event, long tokenUsage) {
        long recordedUsage = event.getTokenUsage() == null ? 0 : event.getTokenUsage();
        event.setTokenUsage(recordedUsage + Math.max(0, tokenUsage));
    }

    private void updateWorkflowTokenUsage(WorkflowEntity workflow) {
        long totalUsage = workflowEventRepository.sumTokenUsageByWorkflowIdAndAgentIn(
                workflow.getWorkflowId(),
                List.of(AgentType.EXTRACTION, AgentType.REVIEW));
        workflow.setTokenUsage(totalUsage);
        workflowRepository.save(workflow);
    }
}