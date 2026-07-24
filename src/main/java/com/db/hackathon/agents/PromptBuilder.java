package com.db.hackathon.agents;

import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.model.document.DocumentAnalysis;
import com.db.hackathon.model.document.DocumentPage;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Component
public class PromptBuilder {

    private static final String SEPARATOR = "\n\n";
    private static final String DOCUMENT_SECTION = "\n\n=== DOCUMENT TEXT ===\n";
    private static final String DEAL_JSON_SECTION = "\n\n=== EXTRACTED DEAL JSON ===\n";
    private static final String VALIDATION_SECTION = "\n\n=== VALIDATION ISSUES ===\n";

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
                + SEPARATOR
                + buildDocumentText(analysis);
    }

    public String buildReviewPrompt(DocumentAnalysis analysis, String dealJson, ValidationResult validationIssuesJson) {
        return reviewPrompt
                + DOCUMENT_SECTION
                + buildDocumentText(analysis)
                + DEAL_JSON_SECTION
                + dealJson
                + VALIDATION_SECTION
                + (validationIssuesJson == null ? "[]" : validationIssuesJson);
    }

    private String buildDocumentText(DocumentAnalysis analysis) {
        List<DocumentPage> pages = analysis.getPages();
        if (pages == null || pages.isEmpty()) {
            log.warn("DocumentAnalysis has no pages; falling back to fullText, pageNumber cannot be resolved accurately");
            return analysis.getFullText();
        }

        StringBuilder sb = new StringBuilder();
        int rendered = 0;
        for (DocumentPage page : pages) {
            if (Boolean.TRUE.equals(page.getBlankPage())) {
                log.debug("Skipping blank page {}", page.getPageNumber());
                continue;
            }
            sb.append("\n===== PAGE ")
              .append(page.getPageNumber())
              .append(" =====\n")
              .append(page.getText() == null ? "" : page.getText())
              .append("\n");
            rendered++;
        }
        log.info("Built page-marked document text: totalPages={}, renderedPages={}, chars={}",
                pages.size(), rendered, sb.length());
        if (rendered <= 1) {
            log.warn("Only {} non-blank page(s) rendered; every field will legitimately have pageNumber=1", rendered);
        }
        return sb.toString();
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