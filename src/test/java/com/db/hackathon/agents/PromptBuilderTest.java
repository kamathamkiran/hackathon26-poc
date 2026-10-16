package com.db.hackathon.agents;

import com.db.hackathon.enums.ValidationCode;
import com.db.hackathon.model.validation.ValidationIssue;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.model.validation.ValidationSeverity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PromptBuilderTest {

    @Test
    void loadsDetailedExtractionAndReviewPromptsWithoutSeparateInstructionFile() {
        PromptBuilder promptBuilder = new PromptBuilder(new ObjectMapper());
        promptBuilder.loadPrompts();

        String extractionPrompt = promptBuilder.buildExtractionPrompt();
        String reviewPrompt = promptBuilder.buildReviewPrompt("{}", null);

        assertTrue(extractionPrompt.contains("The PDF is the sole source of truth"));
        assertTrue(extractionPrompt.contains("Every actual facility must have at least one facility-specific pricing option"));
        assertTrue(extractionPrompt.contains("globalDealProposedCommitmentAmount"));
        assertTrue(reviewPrompt.contains("Validation issues are diagnostic hints for where to investigate"));
        assertTrue(reviewPrompt.contains("Every actual facility must have at least one directly supported facility-specific pricing option"));
        assertTrue(reviewPrompt.contains("Return only the corrected deal JSON"));
    }

    @Test
    void serializesValidationIssuesAsJsonForReviewPrompt() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        PromptBuilder promptBuilder = new PromptBuilder(objectMapper);
        promptBuilder.loadPrompts();
        ValidationResult result = ValidationResult.builder()
                .valid(false)
                .errors(java.util.List.of(ValidationIssue.builder()
                        .field("dealName")
                        .code(ValidationCode.MISSING_FIELD)
                        .severity(ValidationSeverity.ERROR)
                        .message("Deal Name is mandatory")
                        .build()))
                .build();

        String prompt = promptBuilder.buildReviewPrompt("{}", result);
        String validationJson = prompt.substring(
                prompt.lastIndexOf("=== VALIDATION ISSUES ===")
                        + "=== VALIDATION ISSUES ===".length()).trim();
        var validation = objectMapper.readTree(validationJson);

        assertFalse(validation.path("valid").asBoolean());
        assertEquals("dealName", validation.at("/errors/0/field").asText());
        assertEquals("MISSING_FIELD", validation.at("/errors/0/code").asText());
    }
}
