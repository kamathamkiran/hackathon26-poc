package com.db.hackathon.agents.extraction;

import com.db.hackathon.agents.PromptBuilder;
import com.google.adk.agents.LlmAgent;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Getter
@Configuration
public class SemanticConfiguration {

    private LlmAgent extractionAgent;
    private final PromptBuilder promptBuilder;

    @Value("${open-router.ai.model}")
    private String model;

    public SemanticConfiguration(PromptBuilder promptBuilder) {
        this.promptBuilder = promptBuilder;
    }

    @PostConstruct
    public void initialize() throws IOException {

        extractionAgent =
                LlmAgent.builder()
                        .name("ExtractionAgent")
                        .description("Loan Agreement Extraction Agent")
                        .model(model)
                        .instruction(promptBuilder.getInstructionPrompt())
                        .build();

    }

}