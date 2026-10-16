package com.db.hackathon.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.entity.WorkflowEventKey;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.EventStatus;
import com.db.hackathon.enums.WorkflowStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

@DataJpaTest
class WorkflowPersistenceRepositoryTest {

    @Autowired
    private WorkflowEventRepository workflowEventRepository;

    @Autowired
    private WorkflowRepository workflowRepository;

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void stageIdsCanRepeatAcrossDifferentWorkflows() {
        workflowEventRepository.save(event("workflow-a", AgentType.UPLOAD));
        workflowEventRepository.save(event("workflow-b", AgentType.UPLOAD));

        assertTrue(workflowEventRepository.findById(new WorkflowEventKey("workflow-a", 1)).isPresent());
        assertTrue(workflowEventRepository.findById(new WorkflowEventKey("workflow-b", 1)).isPresent());
        assertEquals(2, workflowEventRepository.count());
    }

    @Test
    void workflowStatusAdvancesOnlyFromTheExpectedStage() {
        workflowRepository.save(WorkflowEntity.builder()
                .workflowId("workflow-status")
                .username("test-user")
                .status(WorkflowStatus.UPLOADING)
                .build());

        int updated = workflowRepository.transitionStatus(
                "workflow-status",
                WorkflowStatus.UPLOADING,
                WorkflowStatus.EXTRACTING,
                null,
                LocalDateTime.now());
        int staleUpdate = workflowRepository.transitionStatus(
                "workflow-status",
                WorkflowStatus.UPLOADING,
                WorkflowStatus.FAILED,
                "should not overwrite the next stage",
                LocalDateTime.now());

        assertEquals(1, updated);
        assertEquals(0, staleUpdate);
        entityManager.clear();
        assertEquals(WorkflowStatus.EXTRACTING,
                workflowRepository.findById("workflow-status").orElseThrow().getStatus());
    }

    @Test
    void storesPerAgentTokenUsageAndAggregatesExtractionAndReviewTotals() {
        workflowRepository.save(WorkflowEntity.builder()
                .workflowId("workflow-tokens")
                .username("test-user")
                .status(WorkflowStatus.EXTRACTING)
                .tokenUsage(165L)
                .build());
        workflowEventRepository.save(WorkflowEventEntity.builder()
                .workflowId("workflow-tokens")
                .id(AgentType.EXTRACTION.getWorkflowEventId())
                .agent(AgentType.EXTRACTION)
                .status(EventStatus.STARTED)
                .tokenUsage(120L)
                .build());
        workflowEventRepository.save(WorkflowEventEntity.builder()
                .workflowId("workflow-tokens")
                .id(AgentType.REVIEW.getWorkflowEventId())
                .agent(AgentType.REVIEW)
                .status(EventStatus.STARTED)
                .tokenUsage(45L)
                .build());
        workflowEventRepository.save(WorkflowEventEntity.builder()
                .workflowId("workflow-tokens")
                .id(AgentType.VALIDATION.getWorkflowEventId())
                .agent(AgentType.VALIDATION)
                .status(EventStatus.STARTED)
                .tokenUsage(900L)
                .build());

        entityManager.flush();
        entityManager.clear();

        assertEquals(120L, workflowEventRepository.findById(
                new WorkflowEventKey("workflow-tokens", AgentType.EXTRACTION.getWorkflowEventId()))
                .orElseThrow().getTokenUsage());
        assertEquals(165L, workflowRepository.findById("workflow-tokens")
                .orElseThrow().getTokenUsage());
        assertEquals(165L, workflowEventRepository.sumTokenUsageByWorkflowIdAndAgentIn(
                "workflow-tokens", java.util.List.of(AgentType.EXTRACTION, AgentType.REVIEW)));
    }

    private WorkflowEventEntity event(String workflowId, AgentType agent) {
        return WorkflowEventEntity.builder()
                .workflowId(workflowId)
                .id(agent.getWorkflowEventId())
                .agent(agent)
                .status(EventStatus.STARTED)
                .build();
    }
}
