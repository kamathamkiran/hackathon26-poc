package com.db.hackathon.entity;

import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.EventStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "workflow_event")
@IdClass(WorkflowEventKey.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkflowEventEntity {

    @Id
    @Column(name = "workflow_id", nullable = false, length = 36)
    private String workflowId;

    @Id
    @Column(nullable = false)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AgentType agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EventStatus status;

    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Long durationMs = 0L;

    @Column(name = "token_usage", nullable = false)
    @Builder.Default
    private Long tokenUsage = 0L;

    @Lob
    @Column(name = "failure_reason")
    private String failureReason;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
