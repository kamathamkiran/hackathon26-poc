package com.db.hackathon.workflow;

import com.db.hackathon.adk.agent.*;
import com.db.hackathon.adk.agent.document.DocumentParserAgent;
import com.db.hackathon.entity.WorkflowEntity;
import com.db.hackathon.enums.WorkflowStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkflowEngine {

    private final WorkflowManager workflowManager;

    private final DocumentParserAgent documentParserAgent;

    private final ExtractionAgent extractionAgent;

    private final ValidationAgent validationAgent;

    public WorkflowContext execute(MultipartFile pdf) {

        WorkflowEntity workflow =
                workflowManager.createWorkflow(pdf);

        MDC.put("workflowId", workflow.getWorkflowId());

        WorkflowContext context =
                WorkflowContext.builder()
                        .workflow(workflow)
                        .pdf(pdf)
                        .build();

        try {

            log.info("Workflow started");

            parseDocument(context);

            extractAgreement(context);

            validateAgreement(context);

            workflowManager.updateStatus(
                    workflow,
                    WorkflowStatus.COMPLETED,
                    "Workflow completed successfully");

            log.info("Workflow completed successfully");

            return context;

        } catch (Exception ex) {

            log.error("Workflow failed", ex);

            workflowManager.markFailed(workflow, ex);

            throw new RuntimeException(
                    "Workflow execution failed",
                    ex);

        } finally {

            MDC.remove("workflowId");

        }

    }

    private void parseDocument(
            WorkflowContext context) throws Exception {

        workflowManager.updateStatus(
                context.getWorkflow(),
                WorkflowStatus.PARSING,
                "Started document parsing");

        documentParserAgent.process(context);

        workflowManager.updateStatus(
                context.getWorkflow(),
                WorkflowStatus.PARSED,
                String.format(
                        "Parsed %d pages",
                        context.getDocumentAnalysis()
                                .getTotalPages()));

    }

    private void extractAgreement(
            WorkflowContext context) throws Exception {

        workflowManager.updateStatus(
                context.getWorkflow(),
                WorkflowStatus.EXTRACTING,
                "Started agreement extraction");

        extractionAgent.process(context);

        workflowManager.updateStatus(
                context.getWorkflow(),
                WorkflowStatus.EXTRACTED,
                "Agreement extracted successfully");

    }

    private void validateAgreement(
            WorkflowContext context) throws Exception {

        workflowManager.updateStatus(
                context.getWorkflow(),
                WorkflowStatus.VALIDATING,
                "Started validation");

        validationAgent.process(context);

        workflowManager.updateStatus(
                context.getWorkflow(),
                WorkflowStatus.VALIDATED,
                String.format(
                        "Validation completed. Errors=%d, Warnings=%d",
                        context.getValidationResult()
                                .getErrors()
                                .size(),
                        context.getValidationResult()
                                .getWarnings()
                                .size()));

    }

}