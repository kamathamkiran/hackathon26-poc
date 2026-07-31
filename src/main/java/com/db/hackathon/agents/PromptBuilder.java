package com.db.hackathon.agents;

import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.document.DocumentPage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PromptBuilder {

    private static final String DOCUMENT_SECTION = "\n\n=== DOCUMENT PAGES (JSON) ===\n";
    private static final String DEAL_JSON_SECTION = "\n\n=== EXTRACTED DEAL JSON ===\n";
    private static final String VALIDATION_SECTION = "\n\n=== VALIDATION ISSUES ===\n";

    private final ObjectMapper objectMapper;

    private String extractionPrompt;
    private String reviewPrompt;
    @Getter
    private String instructionPrompt;

    @PostConstruct
    void loadPrompts() {
        instructionPrompt = read("instruction.md");
        extractionPrompt = read("extraction.md");
        reviewPrompt = read("review.md");
        log.info("Loaded extraction and review prompt templates");
    }

    public String buildExtractionPrompt(DocumentAnalysis analysis) {
        return extractionPrompt
                + DOCUMENT_SECTION
                + buildDocumentPagesJson(analysis);
    }

    public String buildReviewPrompt(DocumentAnalysis analysis, String dealJson, ValidationResult validationIssuesJson) {
        return reviewPrompt
                + DOCUMENT_SECTION
                + buildDocumentPagesJson(analysis)
                + DEAL_JSON_SECTION
                + dealJson
                + VALIDATION_SECTION
                + (validationIssuesJson == null ? "[]" : validationIssuesJson);
    }

    private String buildDocumentPagesJson(DocumentAnalysis analysis) {
        List<DocumentPage> pages = analysis == null ? null : analysis.getPages();
        if (pages == null || pages.isEmpty()) {
            log.warn("DocumentAnalysis has no pages; pageNumber cannot be resolved accurately");
            return "[]";
        }

        ArrayNode array = objectMapper.createArrayNode();
        for (DocumentPage page : pages) {
            if (page.getText() == null) {
                continue;
            }
            ObjectNode node = objectMapper.createObjectNode();
            node.put("page", page.getPageNumber());
            node.put("content", page.getText());
            array.add(node);
        }

        try {
            String json = objectMapper.writeValueAsString(array);
            log.debug("Built document pages JSON with {} page(s)", array.size());
            return json;
        } catch (IOException e) {
            log.error("Failed to serialize document pages to JSON", e);
            return "[]";
        }
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