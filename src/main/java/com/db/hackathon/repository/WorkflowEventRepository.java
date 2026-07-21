package com.db.hackathon.repository;

import com.db.hackathon.entity.WorkflowEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowEventRepository
        extends JpaRepository<WorkflowEventEntity, Long> {
}
