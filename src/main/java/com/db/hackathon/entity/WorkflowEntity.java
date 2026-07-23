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

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkflowStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "next_agent")
    private AgentType nextAgent;

    @Column(name = "metadata")
    private String metadata;

    @Lob
    @Column(name="human_output")
    private String humanJson;

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

    public WorkflowEntity(String id, String opsUser, WorkflowStatus workflowStatus, AgentType agentType, String jsonOutput) {
        this.workflowId = id;
        this.username = opsUser;
        this.status = workflowStatus;
        this.nextAgent = agentType;
        this.metadata = jsonOutput;
    }
}
