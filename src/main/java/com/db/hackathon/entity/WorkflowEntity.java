package com.db.hackathon.entity;

import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "workflow")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEntity {

    @Id
    @Column(name = "workflow_id", nullable = false, length = 36)
    private String workflowId;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String fileName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_completed_agent")
    private AgentType lastCompletedAgent;

    @Lob
    @Column(name = "json_output")
    private String jsonOutput;

    @Lob
    @Column(name = "failure_reason")
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime completedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public WorkflowEntity(String id, String opsUser, String fileName, WorkflowStatus workflowStatus, AgentType agentType, String jsonOutput) {
        this.workflowId = id;
        this.username = opsUser;
        this.fileName = fileName;
        this.status = workflowStatus;
        this.lastCompletedAgent = agentType;
        this.jsonOutput = jsonOutput;
    }
}
