package com.db.hackathon.repository;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.WorkflowStatus;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {

    @Modifying
    @Transactional
    @Query("""
       UPDATE WorkflowEntity w
       SET w.status = :status, w.failureReason = :failureReason, w.updatedAt = :updatedAt
       WHERE w.workflowId = :workflowId AND w.status = :expectedStatus
       """)
    int transitionStatus(
            @Param("workflowId") String workflowId,
            @Param("expectedStatus") WorkflowStatus expectedStatus,
            @Param("status") WorkflowStatus status,
            @Param("failureReason") String failureReason,
            @Param("updatedAt") LocalDateTime updatedAt);

    @Query("""
       SELECT w
       FROM WorkflowEntity w
       WHERE w.status <> 'COMPLETED'
         AND w.status <> 'FAILED'
       ORDER BY w.updatedAt
       """)
    List<WorkflowEntity> findPendingWorkflows();
}
