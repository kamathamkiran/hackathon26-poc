package com.db.hackathon.agents.extraction;

import com.db.hackathon.agents.PromptBuilder;
import com.db.hackathon.model.validation.ValidationResult;
import com.google.adk.agents.RunConfig;
import com.google.adk.artifacts.InMemoryArtifactService;
import com.google.adk.events.Event;
import com.google.adk.memory.InMemoryMemoryService;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Blob;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;

@Slf4j
@Service
@RequiredArgsConstructor
public class SemanticRunnerService {

    private final SemanticConfiguration configuration;
    private final PromptBuilder promptBuilder;
    private final ObjectMapper objectMapper;

    @Value("${open-router.ai.app-name}")
    private String appName;

    @Value("${open-router.ai.user-id}")
    private String userId;

    private Runner runner;

    private InMemorySessionService sessionService;
    private static final String PDF_MIME_TYPE = "application/pdf";

    @PostConstruct
    public void initialize() {

        sessionService = new InMemorySessionService();

        runner = Runner.builder()
                .agent(configuration.getExtractionAgent())
                .appName(appName)
                .artifactService(new InMemoryArtifactService())
                .sessionService(sessionService)
                .memoryService(new InMemoryMemoryService())
                .build();

    }

    public AgentResult<JsonNode> extractAgreement(
            byte[] pdfBytes)
            throws Exception {

        String sessionId = createSession();

        String prompt = promptBuilder.buildExtractionPrompt();

        ExecutionResult execution = executeWithPdf(prompt, sessionId, pdfBytes);
        String response = execution.response();

        response = clean(response);

        return new AgentResult<>(
                readDeal(response),
                execution.tokenUsage());

    }

    /** Runs a second-stage critic prompt through the configured ADK model. */
    public String runPrompt(String prompt) throws Exception {
        return clean(execute(prompt, createSession()).response());
    }

    /**
     * Re-reads the source document and reconciles the previously extracted deal, returning an
     * updated deal JSON with corrected value / confidence / pageNumber / sourceText where needed.
     */
    public AgentResult<JsonNode> reviewAgreement(
            byte[] pdfBytes,
            String currentDealJson,
            ValidationResult validationIssuesJson)
            throws Exception {

        String sessionId = createSession();

        String prompt = promptBuilder.buildReviewPrompt(
                currentDealJson, validationIssuesJson);

        ExecutionResult execution = executeWithPdf(prompt, sessionId, pdfBytes);
        String response = clean(execution.response());

        return new AgentResult<>(
                readDeal(response),
                execution.tokenUsage());
    }

    private JsonNode readDeal(String response) throws Exception {
        JsonNode deal = objectMapper.readTree(response);
        if (deal == null || !deal.isObject()) {
            throw new IllegalArgumentException("The extraction response must be a JSON object");
        }
        return deal;
    }

    private ExecutionResult execute(
            String prompt,
            String sessionId)
            throws Exception {
        return execute(sessionId, Part.builder().text(prompt).build());
    }

    private ExecutionResult executeWithPdf(
            String prompt,
            String sessionId,
            byte[] pdfBytes)
            throws Exception {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new IllegalArgumentException("PDF content is missing or empty");
        }

        Part promptPart = Part.builder().text(prompt).build();
        Part pdfPart = Part.builder()
                .inlineData(Blob.builder()
                        .mimeType(PDF_MIME_TYPE)
                        .data(pdfBytes)
                        .build())
                .build();

        return execute(sessionId, promptPart, pdfPart);
    }

    private ExecutionResult execute(
            String sessionId,
            Part... parts)
            throws Exception {

        Content content =
                Content.builder()
                        .role("user")
                        .parts(parts)
                        .build();

        StringBuilder answer = new StringBuilder();
        long tokenUsage = 0;

        Flowable<Event> events =
                runner.runAsync(
                        userId,
                        sessionId,
                        content,
                        RunConfig.builder().build());

        for (Event event : events.blockingIterable()) {

            tokenUsage += event.usageMetadata()
                    .map(usage -> usage.totalTokenCount()
                            .map(Integer::longValue)
                            .orElseGet(() -> (long) usage.promptTokenCount().orElse(0)
                                    + usage.candidatesTokenCount().orElse(0)))
                    .orElse(0L);

            if (event.content().isPresent()) {

                Content response = event.content().get();

                if (response.parts().isPresent()) {

                    response.parts().get()
                        .forEach(part -> {
                            if (part.text().isPresent()) {
                                answer.append(part.text().get());
                            }
                        });
                }

            }

        }

        return new ExecutionResult(answer.toString(), tokenUsage);

    }

    private String createSession()
            throws Exception {

        Session session = sessionService.createSession(appName, userId).blockingGet();

        return session.id();

    }

    private String clean(String response) {

        response = response.trim();
        response = response.replace("```json", "");
        response = response.replace("```", "");

        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");

        if (start >= 0 && end > start) {
            response = response.substring(start, end + 1);
        }

        return response.trim();
    }

    public record AgentResult<T>(T value, long tokenUsage) {
    }

    private record ExecutionResult(String response, long tokenUsage) {
    }

}
