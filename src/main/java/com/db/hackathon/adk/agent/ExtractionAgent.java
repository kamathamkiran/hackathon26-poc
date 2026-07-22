package com.db.hackathon.adk.agent;

import com.db.hackathon.adk.AdkRunnerService;
import com.db.hackathon.model.extraction.Agreement;
import com.db.hackathon.workflow.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExtractionAgent
        implements WorkflowAgent<WorkflowContext, WorkflowContext> {

    private final AdkRunnerService runnerService;

    @Override
    public WorkflowContext process(
            WorkflowContext context)
            throws Exception {

        log.info("Starting agreement extraction");

        Agreement agreement =
                runnerService.extractAgreement(
                        context.getDocumentAnalysis());

        context.setAgreement(agreement);

        log.info("Agreement extraction completed");

        return context;

    }

}
