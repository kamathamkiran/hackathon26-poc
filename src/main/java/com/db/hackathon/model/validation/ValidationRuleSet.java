package com.db.hackathon.model.validation;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ValidationRuleSet {

    private double lowConfidenceThreshold = 0.90;

    private Map<String, List<FieldRule>> sections = new HashMap<>();

    public List<FieldRule> section(String name) {
        return sections.getOrDefault(name, List.of());
    }
}
