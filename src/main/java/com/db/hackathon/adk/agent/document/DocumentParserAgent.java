package com.db.hackathon.adk.agent.document;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.workflow.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParserAgent
        implements WorkflowAgent<WorkflowContext, WorkflowContext> {

    private final GoogleDocumentAiProcessor processor;

    @Override
    public WorkflowContext process(
            WorkflowContext context)
            throws Exception {

        DocumentAnalysis analysis =
                processor.process(context.getPdf());

        context.setDocumentAnalysis(analysis);

        return context;
    }

}