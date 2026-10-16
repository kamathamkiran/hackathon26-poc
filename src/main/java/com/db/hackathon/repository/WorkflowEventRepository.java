package com.db.hackathon.repository;

import com.db.hackathon.entity.WorkflowEventEntity;
import com.db.hackathon.entity.WorkflowEventKey;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.EventStatus;
import java.util.Collection;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowEventRepository extends JpaRepository<WorkflowEventEntity, WorkflowEventKey> {

    Optional<WorkflowEventEntity> findTopByWorkflowIdAndAgentOrderByUpdatedAtDesc(
            String workflowId,
            AgentType agent);

    Optional<WorkflowEventEntity> findTopByWorkflowIdAndStatusOrderByUpdatedAtDesc(
            String workflowId,
            EventStatus status);

    Optional<WorkflowEventEntity> findTopByWorkflowIdOrderByUpdatedAtDesc(String workflowId);

    @Query("""
            SELECT COALESCE(SUM(we.tokenUsage), 0)
            FROM WorkflowEventEntity we
            WHERE we.workflowId = :workflowId
              AND we.agent IN :agents
            """)
    long sumTokenUsageByWorkflowIdAndAgentIn(
            @Param("workflowId") String workflowId,
            @Param("agents") Collection<AgentType> agents);
}
