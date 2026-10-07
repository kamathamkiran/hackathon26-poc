package com.db.hackathon.repository;

import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.EventStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkflowEventRepository extends JpaRepository<WorkflowEventEntity, String> {

    Optional<WorkflowEventEntity> findTopByWorkflowIdAndAgentOrderByUpdatedAtDesc(
            String workflowId,
            AgentType agent);

    Optional<WorkflowEventEntity> findTopByWorkflowIdAndStatusOrderByUpdatedAtDesc(
            String workflowId,
            EventStatus status);

    Optional<WorkflowEventEntity> findTopByWorkflowIdOrderByUpdatedAtDesc(String workflowId);
}
