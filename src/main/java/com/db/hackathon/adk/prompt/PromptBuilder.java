package com.db.hackathon.adk.prompt;

import com.db.hackathon.model.document.DocumentAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.db.hackathon.enums.AgentType.EXTRACTION;
import static com.db.hackathon.enums.AgentType.REVIEW;

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

    public String buildReviewPrompt(
            DocumentAnalysis analysis,
            String dealJson) {

        return promptLoader.load(REVIEW)
                + "\n\n=== DOCUMENT TEXT ===\n"
                + analysis.getFullText()
                + "\n\n=== EXTRACTED DEAL JSON ===\n"
                + dealJson;
    }

}