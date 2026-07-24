package com.db.hackathon.adk.agent.validation;

import com.db.hackathon.model.validation.ValidationRuleSet;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class ValidationRuleProvider {

    private static final String RULES_RESOURCE = "validation-rules.json";

    private final ValidationRuleSet ruleSet;

    public ValidationRuleProvider(ObjectMapper objectMapper) {
        this.ruleSet = load(objectMapper);
    }

    public ValidationRuleSet ruleSet() {
        return ruleSet;
    }

    private ValidationRuleSet load(ObjectMapper objectMapper) {
        try (InputStream in = new ClassPathResource(RULES_RESOURCE).getInputStream()) {
            ValidationRuleSet loaded = objectMapper.readValue(in, ValidationRuleSet.class);
            log.info("Loaded validation rules: {} section(s)", loaded.getSections().size());
            return loaded;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Unable to load validation rules from " + RULES_RESOURCE, ex);
        }
    }
}
