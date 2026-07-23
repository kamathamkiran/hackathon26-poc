package com.db.hackathon.workflow;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.EventStatus;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.repository.WorkflowEventRepository;
import com.db.hackathon.repository.WorkflowRepository;
import com.db.hackathon.service.JsonSerializerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static java.time.ZoneId.systemDefault;

@Service
@RequiredArgsConstructor
public class WorkflowManager {

    private static final int MAX_RETRY = 3;

    private final WorkflowRepository workflowRepository;
    private final WorkflowEventRepository workflowEventRepository;
    private final JsonSerializerService jsonSerializer;

    public void saveCheckpoint(
            WorkflowEntity workflow,
            WorkflowStatus status,
            AgentType completedAgent,
            Object output) {

        workflow.setStatus(status);
        workflow.setLastCompletedAgent(completedAgent);
        workflow.setJsonOutput(jsonSerializer.serialize(output));
        workflowRepository.save(workflow);
    }

    public void markCompleted(WorkflowEntity workflow) {

        workflow.setStatus(WorkflowStatus.COMPLETED);
        workflow.setCompletedAt(LocalDateTime.now());

        workflowRepository.save(workflow);
    }

    public void markFailed(
            WorkflowEntity workflow,
            String reason) {

        workflow.setStatus(WorkflowStatus.FAILED);
        workflow.setFailureReason(reason);

        workflowRepository.save(workflow);
    }

    public boolean incrementRetry(
            WorkflowEventEntity event,
            String reason) {

        long durationMs = System.currentTimeMillis() - event.getCreatedAt().atZone(systemDefault()).toInstant().toEpochMilli();

        event.setStatus(EventStatus.FAILED);
        event.setDurationMs(durationMs);
        event.setRetryCount(event.getRetryCount() + 1);
        event.setFailureReason(reason);

        workflowEventRepository.save(event);

        return event.getRetryCount() < MAX_RETRY;
    }

    public void completeEvent(
            WorkflowEventEntity event,
            long durationMs) {

        event.setStatus(EventStatus.SUCCESS);
        event.setDurationMs(durationMs);

        workflowEventRepository.save(event);
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
                                        .id(UUID.randomUUID().toString())
                                        .workflowId(workflowId)
                                        .agent(agent)
                                        .status(EventStatus.STARTED)
                                        .retryCount(0)
                                        .build()));
    }
}