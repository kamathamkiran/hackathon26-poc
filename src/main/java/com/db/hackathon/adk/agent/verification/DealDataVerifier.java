package com.db.hackathon.adk.agent.verification;

import com.db.hackathon.model.verification.DealDataVerificationResult;
import tools.jackson.databind.JsonNode;
import com.networknt.schema.Error;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SpecificationVersion;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Deterministic gate for the JSON produced by the extraction workflow. */
@Component
@RequiredArgsConstructor
public class DealDataVerifier {

    private static final String SCHEMA_RESOURCE = "schema.json";

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DealDataVerificationResult verify(String json) {
        List<String> errors = new ArrayList<>();
        tools.jackson.databind.JsonNode deal;

        try {
            deal = objectMapper.readTree(json);
        } catch (Exception exception) {
            errors.add("Extracted deal data is not valid JSON: " + message(exception));
            return result(errors);
        }

        if (deal == null || !deal.isObject()) {
            errors.add("Extracted deal data must be a JSON object");
            return result(errors);
        }

        try {
            JsonNode schemaDocument = readResource(SCHEMA_RESOURCE);
            JsonNode schemaNode = schemaDocument.path("schema");
            if (!schemaNode.isObject()) {
                errors.add("schema.json must contain an object at the 'schema' path");
                return result(errors);
            }

            Schema schema = SchemaRegistry.withDefaultDialect(SpecificationVersion.DRAFT_7)
                    .getSchema(String.valueOf(schemaNode));
            for (Error error : schema.validate(deal)) {
                errors.add("JSON Schema " + error.getInstanceLocation() + ": " + error.getMessage());
            }

            validateDescriptionRules(deal, errors);
            validateDuplicates(deal, errors);
            validateDateRelationships(deal, errors);
            validateCommitmentTotals(deal, errors);
        } catch (Exception exception) {
            errors.add("Unable to validate extracted deal data: " + message(exception));
        }

        return result(errors);
    }

    private DealDataVerificationResult result(List<String> errors) {
        return DealDataVerificationResult.builder()
                .valid(errors.isEmpty())
                .errors(errors)
                .build();
    }

    private tools.jackson.databind.JsonNode readResource(String resourceName) throws IOException {
        ClassPathResource resource = new ClassPathResource(resourceName);
        try (var inputStream = resource.getInputStream()) {
            return objectMapper.readTree(inputStream);
        }
    }

    /**
     * Reads the {@code value} of an {@link com.db.hackathon.model.extraction.ExtractedField}
     * wrapper nested under {@code parent.field}. Returns {@code null} when the value is absent.
     */
    private String fieldValue(JsonNode parent, String field) {
        JsonNode wrapper = parent.path(field);
        JsonNode value = wrapper.path("value");
        if (value.isMissingNode() || value.isNull() || !value.isValueNode()) {
            return null;
        }
        String text = value.asText();
        return text.isEmpty() ? null : text;
    }

    private void validateDescriptionRules(JsonNode deal, List<String> errors) {
        validateAlphanumericWithSpaces(deal, "dealName", errors);
        validateAlphanumericWithSpaces(deal, "currency", errors);
        validateAlphanumericWithSpaces(deal, "department", errors);
        validateMaxLength(deal, "dealName", 40, "deal", errors);
        validateMaxLength(deal.path("dealAdminAgent"), "customerExternalId", 15,
                "dealAdminAgent", errors);
        validateMaxLength(deal.path("dealBorrower"), "customerExternalId", 15,
                "dealBorrower", errors);

        JsonNode facilities = deal.path("facilityList");
        for (int i = 0; i < facilities.size(); i++) {
            JsonNode facility = facilities.get(i);
            validateMaxLength(facility, "dealTrackingNumber", 12,
                    "facilityList[" + i + "]", errors);
            validateMaxLength(facility, "facilityName", 30,
                    "facilityList[" + i + "]", errors);
        }
    }

    private void validateAlphanumericWithSpaces(JsonNode parent, String field, List<String> errors) {
        String value = fieldValue(parent, field);
        if (value != null && !value.matches("[A-Za-z0-9]+(?:[ ][A-Za-z0-9]+)*")) {
            errors.add("Description rule " + field
                    + " must contain only letters, numbers, and single spaces");
        }
    }

    private void validateMaxLength(JsonNode parent, String field, int max,
                                   String prefix, List<String> errors) {
        String value = fieldValue(parent, field);
        if (value != null && value.length() > max) {
            errors.add("Description rule " + prefix + "." + field
                    + " exceeds maxLength " + max);
        }
    }

    private void validateDuplicates(JsonNode deal, List<String> errors) {
        findDuplicateObjects(deal.path("interestPricingOptions"), "interestPricingOptions", errors);
        findDuplicateObjects(deal.path("facilityList"), "facilityList", errors);
        findDuplicateValues(deal.path("facilityList"), "dealTrackingNumber", "facilityList", errors);
    }

    private void findDuplicateObjects(JsonNode array, String path, List<String> errors) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            if (!seen.add(array.get(i).toString())) {
                errors.add("Duplicate entry in " + path + " at index " + i);
            }
        }
    }

    private void findDuplicateValues(JsonNode array, String field, String path, List<String> errors) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < array.size(); i++) {
            String value = fieldValue(array.get(i), field);
            if (value != null && !seen.add(value)) {
                errors.add("Duplicate " + field + " '" + value
                        + "' in " + path + " at index " + i);
            }
        }
    }

    private void validateDateRelationships(JsonNode deal, List<String> errors) {
        JsonNode facilities = deal.path("facilityList");
        for (int i = 0; i < facilities.size(); i++) {
            JsonNode facility = facilities.get(i);
            compareDates(facility, "agreementDate", "effectiveDate",
                    "facilityList[" + i + "]", errors);
            compareDates(facility, "effectiveDate", "expiryDate",
                    "facilityList[" + i + "]", errors);
            compareDates(facility, "expiryDate", "finalMaturityDate",
                    "facilityList[" + i + "]", errors);
        }
    }

    private void compareDates(JsonNode object, String earlierField, String laterField,
                              String path, List<String> errors) {
        String earlierText = fieldValue(object, earlierField);
        String laterText = fieldValue(object, laterField);
        if (earlierText == null || laterText == null) {
            return;
        }
        try {
            LocalDate earlier = LocalDate.parse(earlierText);
            LocalDate later = LocalDate.parse(laterText);
            if (later.isBefore(earlier)) {
                errors.add(path + " requires " + laterField
                        + " to be on or after " + earlierField);
            }
        } catch (DateTimeParseException ignored) {
            // Schema validation reports malformed or missing dates.
        }
    }

    private void validateCommitmentTotals(JsonNode deal, List<String> errors) {
        BigDecimal global = decimal(fieldValue(deal, "globalDealProposedCommitmentAmount"));
        if (global == null) {
            return;
        }

        BigDecimal facilityTotal = sum(deal.path("facilityList"), "proposedCommitmentAmount");
        if (facilityTotal != null && global.compareTo(facilityTotal) != 0) {
            errors.add("facilityList proposedCommitmentAmount total " + facilityTotal
                    + " does not equal globalDealProposedCommitmentAmount " + global);
        }
    }

    private BigDecimal sum(JsonNode array, String field) {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode item : array) {
            BigDecimal value = decimal(fieldValue(item, field));
            if (value == null) {
                return null;
            }
            total = total.add(value);
        }
        return total;
    }

    private BigDecimal decimal(String text) {
        if (text == null) {
            return null;
        }
        try {
            return new BigDecimal(text.replaceAll("[,$\\s]", ""));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String message(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
