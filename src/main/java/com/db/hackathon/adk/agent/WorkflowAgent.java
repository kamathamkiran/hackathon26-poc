package com.db.hackathon.adk.agent;

public interface WorkflowAgent<I, O> {

    O process(I input) throws Exception;

}
