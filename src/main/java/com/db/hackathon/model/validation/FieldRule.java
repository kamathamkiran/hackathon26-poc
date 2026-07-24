package com.db.hackathon.model.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FieldRule {

    private String field;

    private String label;

    private boolean mandatory;

    private FieldType type = FieldType.STRING;

    private Integer maxLength;

    private boolean positive;

    private boolean nonNegative;

    private List<String> allowedValues;
}
