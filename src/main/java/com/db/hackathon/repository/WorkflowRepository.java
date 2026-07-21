package com.db.hackathon.repository;

import com.db.hackathon.entity.WorkflowEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WorkflowRepository
        extends JpaRepository<WorkflowEntity, String> {
}

