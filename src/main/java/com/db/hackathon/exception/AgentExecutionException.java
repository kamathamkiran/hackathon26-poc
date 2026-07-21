package com.db.hackathon.exception;

public class AgentExecutionException
        extends RuntimeException {

    public AgentExecutionException(
            String message,
            Throwable cause) {

        super(message, cause);
    }

}
