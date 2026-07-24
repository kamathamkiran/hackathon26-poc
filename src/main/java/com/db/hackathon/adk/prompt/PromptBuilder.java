package com.db.hackathon.adk.prompt;

import com.db.hackathon.enums.AgentType;
import com.db.hackathon.model.document.DocumentAnalysis;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private static final String SEPARATOR = "\n\n";
    private static final String DOCUMENT_SECTION = "\n\n=== DOCUMENT TEXT ===\n";
    private static final String DEAL_JSON_SECTION = "\n\n=== EXTRACTED DEAL JSON ===\n";

    private final PromptLoader promptLoader;

    public String buildExtractionPrompt(DocumentAnalysis analysis) {
        return promptLoader.load(AgentType.EXTRACTION)
                + SEPARATOR
                + analysis.getFullText();
    }

    public String buildReviewPrompt(DocumentAnalysis analysis, String dealJson) {
        return promptLoader.load(AgentType.REVIEW)
                + DOCUMENT_SECTION
                + analysis.getFullText()
                + DEAL_JSON_SECTION
                + dealJson;
    }

}