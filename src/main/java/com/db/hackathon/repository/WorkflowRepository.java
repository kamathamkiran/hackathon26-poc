package com.db.hackathon.repository;

import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.WorkflowStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface WorkflowRepository extends JpaRepository<WorkflowEntity, String> {

    @Query("""
       SELECT w
       FROM WorkflowEntity w
       WHERE w.status <> 'COMPLETED'
         AND w.status <> 'FAILED'
       ORDER BY w.updatedAt
       """)
    List<WorkflowEntity> findPendingWorkflows();
}

