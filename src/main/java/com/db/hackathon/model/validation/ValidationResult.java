package com.db.hackathon.model.validation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResult {

    private boolean valid;

    @Builder.Default
    private List<ValidationMessage> errors = new ArrayList<>();

    @Builder.Default
    private List<ValidationMessage> warnings = new ArrayList<>();

}
