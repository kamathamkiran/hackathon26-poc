package com.db.hackathon.adk.agent.document;

import com.db.hackathon.adk.agent.WorkflowAgent;
import com.db.hackathon.enums.AgentType;
import com.db.hackathon.enums.WorkflowStatus;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.dto.WorkflowContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Slf4j
@Component
@RequiredArgsConstructor
public class DocumentParserAgent implements WorkflowAgent {

    private final GoogleDocumentAiProcessor processor;

    @Override
    public AgentType getAgentType() {
        return AgentType.DOCUMENT_PARSER;
    }

    @Override
    public WorkflowStatus getInputStatus() {
        return WorkflowStatus.PARSING;
    }

    @Override
    public WorkflowStatus getOutputStatus() {
        return WorkflowStatus.PARSED;
    }

    @Override
    public Object getOutput(WorkflowContext context) {
        return context.getDocumentAnalysis();
    }

    @Override
    public void process(
            WorkflowContext context) throws Exception {

        DocumentAnalysis analysis =
                processor.process(Path.of(context.getFilePath()));

        context.setDocumentAnalysis(analysis);

    }

}