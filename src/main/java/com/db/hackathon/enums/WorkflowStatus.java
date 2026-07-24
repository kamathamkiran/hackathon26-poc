package com.db.hackathon.enums;

public enum WorkflowStatus {

    UPLOADED,

    PARSING,
    PARSED,

    EXTRACTING,
    EXTRACTED,

    VALIDATING,
    VALIDATED,

    REVIEWING,
    REVIEWED,

    HUMAN_REVIEW_PENDING,

    DEAL_CREATED,

    // Retained for backward compatibility.
    DEAL_DATA_VERIFIED,
    CREDIT_AGREEMENT_VERIFIED,

    COMPLETED,

    FAILED
}
