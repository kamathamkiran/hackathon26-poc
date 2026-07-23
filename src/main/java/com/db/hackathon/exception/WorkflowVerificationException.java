package com.db.hackathon.exception;

/** A non-transient verification failure that must fail the workflow closed. */
public class WorkflowVerificationException extends RuntimeException {

    public WorkflowVerificationException(String message) {
        super(message);
    }
}
