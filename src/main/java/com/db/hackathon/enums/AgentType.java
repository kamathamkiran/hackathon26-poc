package com.db.hackathon.enums;

public enum AgentType {

    UPLOAD(1),

    EXTRACTION(2),

    VALIDATION(3),

    REVIEW(4),

    HUMAN_REVIEW(5);

    private final int workflowEventId;

    AgentType(int workflowEventId) {
        this.workflowEventId = workflowEventId;
    }

    public int getWorkflowEventId() {
        return workflowEventId;
    }
}
