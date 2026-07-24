package com.db.hackathon.model.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;

    @Builder.Default
    private List<ValidationIssue> errors = new ArrayList<>();

    @Builder.Default
    private List<ValidationIssue> warnings = new ArrayList<>();

    /** Convenience view combining errors and warnings for the UI/human review. */
    public List<ValidationIssue> allIssues() {
        return Stream.concat(
                        errors == null ? Stream.empty() : errors.stream(),
                        warnings == null ? Stream.empty() : warnings.stream())
                .toList();
    }
}
