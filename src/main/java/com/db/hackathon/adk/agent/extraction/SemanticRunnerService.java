package com.db.hackathon.adk.agent.extraction;

import com.db.hackathon.adk.prompt.PromptBuilder;
import com.db.hackathon.model.extraction.Deal;
import com.google.adk.agents.RunConfig;
import com.google.adk.artifacts.InMemoryArtifactService;
import com.google.adk.events.Event;
import com.google.adk.memory.InMemoryMemoryService;
import com.google.adk.runner.Runner;
import com.google.adk.sessions.InMemorySessionService;
import com.google.adk.sessions.Session;
import com.google.genai.types.Content;
import com.google.genai.types.Part;
import io.reactivex.rxjava3.core.Flowable;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.db.hackathon.model.document.DocumentAnalysis;

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

    public Deal extractAgreement(
            DocumentAnalysis documentAnalysis)
            throws Exception {

        String sessionId = createSession();

        String prompt = promptBuilder.buildExtractionPrompt(documentAnalysis);

        String response = execute(prompt, sessionId);

        response = clean(response);

        return objectMapper.readValue(
                response,
                Deal.class);

    }

    /** Runs a second-stage critic prompt through the configured ADK model. */
    public String runPrompt(String prompt) throws Exception {
        return clean(execute(prompt, createSession()));
    }

    /**
     * Re-reads the source document and reconciles the previously extracted deal, returning an
     * updated {@link Deal} with corrected value / confidence / pageNumber / sourceText where needed.
     */
    public Deal reviewAgreement(
            DocumentAnalysis documentAnalysis,
            String currentDealJson)
            throws Exception {

        String sessionId = createSession();

        String prompt = promptBuilder.buildReviewPrompt(documentAnalysis, currentDealJson);

        String response = clean(execute(prompt, sessionId));

        return objectMapper.readValue(response, Deal.class);
    }

    private String execute(
            String prompt,
            String sessionId)
            throws Exception {

        Content content =
                Content.builder()
                        .role("user")
                        .parts(
                                Part.builder()
                                        .text(prompt)
                                        .build())
                        .build();

        StringBuilder answer = new StringBuilder();

        Flowable<Event> events =
                runner.runAsync(
                        userId,
                        sessionId,
                        content,
                        RunConfig.builder().build());

        for (Event event : events.blockingIterable()) {

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

        return answer.toString();

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

}
