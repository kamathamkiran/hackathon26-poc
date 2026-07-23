package com.db.hackathon.adk.agent.extraction;

import com.google.adk.agents.LlmAgent;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Getter
@Configuration
public class SemanticConfiguration {

    private LlmAgent extractionAgent;

    @Value("${google.ai.model}")
    private String model;

    @PostConstruct
    public void initialize() throws IOException {

        ClassPathResource resource =
                new ClassPathResource("prompts/instruction.md");

        String instructionText = new String(
                resource.getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);

        extractionAgent =
                LlmAgent.builder()
                        .name("ExtractionAgent")
                        .description("Loan Agreement Extraction Agent")
                        .model(model)
                        .instruction(instructionText)
                        .build();

    }

}