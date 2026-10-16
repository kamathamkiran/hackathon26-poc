package com.db.hackathon.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class AgentTypeTest {

    @Test
    void workflowEventIdsMatchTheDocumentedStageOrder() {
        assertEquals(1, AgentType.UPLOAD.getWorkflowEventId());
        assertEquals(2, AgentType.EXTRACTION.getWorkflowEventId());
        assertEquals(3, AgentType.VALIDATION.getWorkflowEventId());
        assertEquals(4, AgentType.REVIEW.getWorkflowEventId());
        assertEquals(5, AgentType.HUMAN_REVIEW.getWorkflowEventId());
    }
}
