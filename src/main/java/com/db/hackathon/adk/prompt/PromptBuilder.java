package com.db.hackathon.adk.prompt;

import com.db.hackathon.model.document.DocumentAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.db.hackathon.enums.AgentType.EXTRACTION;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptLoader promptLoader;

    public String buildExtractionPrompt(
            DocumentAnalysis analysis) {

        return promptLoader.load(EXTRACTION) +
                "\n\n" +
                analysis.getFullText();

    }

}