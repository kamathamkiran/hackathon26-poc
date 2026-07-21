package com.db.hackathon.adk;

import com.google.adk.agents.LlmAgent;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AdkConfiguration {

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
                        .instruction("")
                        .build();

    }

}