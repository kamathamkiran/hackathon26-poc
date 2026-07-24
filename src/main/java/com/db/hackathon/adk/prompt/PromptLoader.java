package com.db.hackathon.adk.prompt;

import com.db.hackathon.enums.AgentType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Map;

@Slf4j
@Component
public class PromptLoader {

    private static final Map<AgentType, String> PROMPT_FILES = new EnumMap<>(AgentType.class);

    static {
        PROMPT_FILES.put(AgentType.DOCUMENT_PARSER, "document.md");
        PROMPT_FILES.put(AgentType.EXTRACTION, "extraction.md");
        PROMPT_FILES.put(AgentType.VALIDATION, "validation.md");
        PROMPT_FILES.put(AgentType.REVIEW, "review.md");
    }

    private final Map<AgentType, String> cache = new EnumMap<>(AgentType.class);

    @PostConstruct
    void preload() {
        PROMPT_FILES.forEach((agentType, file) -> cache.put(agentType, read(file)));
        log.info("Loaded {} prompt template(s)", cache.size());
    }

    public String load(AgentType agentType) {
        String prompt = cache.get(agentType);
        if (prompt == null) {
            throw new IllegalArgumentException("No prompt template configured for agent: " + agentType);
        }
        return prompt;
    }

    private String read(String file) {
        ClassPathResource resource = new ClassPathResource("prompts/" + file);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load prompt template: " + file, e);
        }
    }
}
