package com.db.hackathon.adk.prompt;

import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.document.DocumentPage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import static com.db.hackathon.enums.AdkAgentType.EXTRACTION;

@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private final PromptLoader promptLoader;

    public String buildExtractionPrompt(
            DocumentAnalysis analysis) {

        StringBuilder builder =
                new StringBuilder();

        builder.append(
                promptLoader.load(EXTRACTION));

        builder.append("\n\n");

        for (DocumentPage page : analysis.getPages()) {

            if (page.isBlankPage()) {
                continue;
            }

            builder.append("===== PAGE ")
                    .append(page.getPageNumber())
                    .append(" =====\n");

            builder.append(page.getText());

            builder.append("\n\n");

        }

        return builder.toString();

    }

}