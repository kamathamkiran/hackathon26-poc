package com.db.hackathon.model.validation;

import com.db.hackathon.enums.ValidationCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ValidationIssue {

    private String field;

    private ValidationCode code;

    private ValidationSeverity severity;

    private String message;

    private Integer pageNumber;
}
