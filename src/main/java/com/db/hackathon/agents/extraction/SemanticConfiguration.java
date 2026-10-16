package com.db.hackathon.agents.extraction;

import com.google.adk.agents.LlmAgent;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class SemanticConfiguration {

    private LlmAgent extractionAgent;

    @Value("${open-router.ai.model}")
    private String model;

    @PostConstruct
    public void initialize() {

        extractionAgent =
                LlmAgent.builder()
                        .name("ExtractionAgent")
                        .description("Loan Agreement Extraction Agent")
                        .model(model)
                        .instruction("Follow the complete task-specific instructions supplied with each request. "
                                + "Use the attached PDF as the only source of business data and return only "
                                + "the requested result.")
                        .build();

    }

}