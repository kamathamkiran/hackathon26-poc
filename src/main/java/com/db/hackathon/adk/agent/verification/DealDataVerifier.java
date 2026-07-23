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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Deterministic gate for the JSON produced by the extraction workflow. */
@Component
@RequiredArgsConstructor
public class DealDataVerifier {

    private static final String SCHEMA_RESOURCE = "schema.json";
    private static final String SOURCE_RANGE_PATTERN =
            "^[1-9][0-9]*,[1-9][0-9]*-[1-9][0-9]*,[1-9][0-9]*$";

    private final ObjectMapper objectMapper;

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
            validateSourceLocations(deal, errors);
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

    private void validateSourceLocations(JsonNode deal, List<String> errors) {
        JsonNode locations = deal.path("_sourceLocations");
        if (!locations.isObject()) {
            errors.add("Every deal field must have a page/line range in _sourceLocations");
            return;
        }

        Set<String> expectedPaths = new HashSet<>();
        collectFieldPaths(deal, "", expectedPaths);

        Iterator<Map.Entry<String, JsonNode>> fields = locations.properties().iterator();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> entry = fields.next();
            String path = entry.getKey();
            JsonNode range = entry.getValue();
            if (!expectedPaths.contains(path)) {
                errors.add("_sourceLocations contains a path that is not a deal field: " + path);
            }
            if (!range.isTextual() || !range.asText().matches(SOURCE_RANGE_PATTERN)) {
                errors.add("Invalid page/line range for " + path + ": " + range);
            }
        }

        for (String path : expectedPaths) {
            if (!locations.has(path)) {
                errors.add("Missing page/line range in _sourceLocations for " + path);
            }
        }
    }

    private void collectFieldPaths(JsonNode node, String path, Set<String> paths) {
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.properties().iterator();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (path.isEmpty() && field.getKey().equals("_sourceLocations")) {
                    continue;
                }
                String fieldPath = path.isEmpty() ? field.getKey() : path + "." + field.getKey();
                paths.add(fieldPath);
                collectFieldPaths(field.getValue(), fieldPath, paths);
            }
        } else if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String itemPath = path + "[" + i + "]";
                paths.add(itemPath);
                collectFieldPaths(node.get(i), itemPath, paths);
            }
        }
    }

    private void validateDescriptionRules(JsonNode deal, List<String> errors) {
        validateAlphanumericWithSpaces(deal, "dealName", errors);
        validateAlphanumericWithSpaces(deal, "currency", errors);
        validateAlphanumericWithSpaces(deal, "department", errors);
        validateMaxLength(deal.path("dealAdminAgent"), "customerExternalId", 15,
                "dealAdminAgent", errors);

        JsonNode facilities = deal.path("facilityList");
        for (int i = 0; i < facilities.size(); i++) {
            JsonNode facility = facilities.get(i);
            validateMaxLength(facility, "dealTrackingNumber", 12,
                    "facilityList[" + i + "]", errors);
            validateMaxLength(facility, "facilityName", 30,
                    "facilityList[" + i + "]", errors);
        }

        JsonNode trades = deal.path("tradeList");
        for (int i = 0; i < trades.size(); i++) {
            validateMaxLength(trades.get(i), "dealInternalId", 8,
                    "tradeList[" + i + "]", errors);
        }
    }

    private void validateAlphanumericWithSpaces(JsonNode parent, String field, List<String> errors) {
        JsonNode value = parent.get(field);
        if (value != null && value.isTextual()
                && !value.asText().matches("[A-Za-z0-9]+(?:[ ][A-Za-z0-9]+)*")) {
            errors.add("Description rule " + field
                    + " must contain only letters, numbers, and single spaces");
        }
    }

    private void validateMaxLength(JsonNode parent, String field, int max,
                                   String prefix, List<String> errors) {
        JsonNode value = parent.get(field);
        if (value != null && value.isTextual() && value.asText().length() > max) {
            errors.add("Description rule " + prefix + "." + field
                    + " exceeds maxLength " + max);
        }
    }

    private void validateDuplicates(JsonNode deal, List<String> errors) {
        findDuplicateObjects(deal.path("interestPricingOptions"), "interestPricingOptions", errors);
        findDuplicateObjects(deal.path("facilityList"), "facilityList", errors);
        findDuplicateObjects(deal.path("tradeList"), "tradeList", errors);
        findDuplicateValues(deal.path("facilityList"), "dealTrackingNumber", "facilityList", errors);
        findDuplicateValues(deal.path("tradeList"), "dealInternalId", "tradeList", errors);
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
            JsonNode value = array.get(i).get(field);
            if (value != null && !value.isNull() && value.isTextual()
                    && !seen.add(value.asText())) {
                errors.add("Duplicate " + field + " '" + value.asText()
                        + "' in " + path + " at index " + i);
            }
        }
    }

    private void validateDateRelationships(JsonNode deal, List<String> errors) {
        for (int i = 0; i < deal.path("facilityList").size(); i++) {
            JsonNode facility = deal.path("facilityList").get(i);
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
        try {
            LocalDate earlier = LocalDate.parse(object.path(earlierField).asText());
            LocalDate later = LocalDate.parse(object.path(laterField).asText());
            if (later.isBefore(earlier)) {
                errors.add(path + " requires " + laterField
                        + " to be on or after " + earlierField);
            }
        } catch (DateTimeParseException ignored) {
            // Schema validation reports malformed or missing dates.
        }
    }

    private void validateCommitmentTotals(JsonNode deal, List<String> errors) {
        BigDecimal global = decimal(deal.get("globalDealProposedCommitmentAmount"));
        if (global == null) {
            return;
        }

        BigDecimal facilityTotal = sum(deal.path("facilityList"), "proposedCommitmentAmount");
        if (facilityTotal != null && global.compareTo(facilityTotal) != 0) {
            errors.add("facilityList proposedCommitmentAmount total " + facilityTotal
                    + " does not equal globalDealProposedCommitmentAmount " + global);
        }

        BigDecimal tradeTotal = BigDecimal.ZERO;
        for (JsonNode trade : deal.path("tradeList")) {
            BigDecimal amount = decimal(trade.path("facilityPosition")
                    .path("portfolioAllocation").get("facilityCurrencyAmount"));
            if (amount == null) {
                return;
            }
            tradeTotal = tradeTotal.add(amount);
        }
        if (global.compareTo(tradeTotal) != 0) {
            errors.add("tradeList facilityCurrencyAmount total " + tradeTotal
                    + " does not equal globalDealProposedCommitmentAmount " + global);
        }
    }

    private BigDecimal sum(JsonNode array, String field) {
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode item : array) {
            BigDecimal value = decimal(item.get(field));
            if (value == null) {
                return null;
            }
            total = total.add(value);
        }
        return total;
    }

    private BigDecimal decimal(JsonNode node) {
        return node != null && node.isNumber() ? node.decimalValue() : null;
    }

    private String message(Exception exception) {
        return exception.getMessage() == null
                ? exception.getClass().getSimpleName()
                : exception.getMessage();
    }
}
