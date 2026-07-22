package com.db.hackathon.adk.agent.validation;

import com.db.hackathon.model.extraction.Agreement;
import com.db.hackathon.model.extraction.ExtractedField;
import com.db.hackathon.model.validation.ValidationMessage;
import com.db.hackathon.model.validation.ValidationResult;
import org.springframework.stereotype.Component;

@Component
public class ValidationService {

    private static final double CONFIDENCE_THRESHOLD = 0.80;

    public void validateMandatoryFields(
            ValidationResult result,
            Agreement agreement) {

        for (AgreementField field : AgreementField.values()) {

            ExtractedField extracted = agreement.getField(field);

            if (field.isMandatory()) {
                validateMandatory(result, field, extracted);
            }

            validateConfidence(result, field, extracted);
        }

    }

    private void validateMandatory(
            ValidationResult result,
            AgreementField field,
            ExtractedField extracted) {

        if (extracted == null ||
                extracted.getValue() == null ||
                extracted.getValue().isBlank()) {

            result.getErrors().add(
                    ValidationMessage.builder()
                            .field(field.name())
                            .code(ValidationCode.MISSING_FIELD)
                            .message(field.name() + " is mandatory")
                            .build());
        }
    }

    private void validateConfidence(
            ValidationResult result,
            AgreementField field,
            ExtractedField extracted) {

        if (extracted == null ||
                extracted.getConfidence() == null) {
            return;
        }

        if (extracted.getConfidence() < CONFIDENCE_THRESHOLD) {

            result.getWarnings().add(
                    ValidationMessage.builder()
                            .field(field.name())
                            .pageNumber(extracted.getPageNumber())
                            .code(ValidationCode.LOW_CONFIDENCE)
                            .message(field.name() + " confidence below threshold")
                            .build());

        }
    }
}
