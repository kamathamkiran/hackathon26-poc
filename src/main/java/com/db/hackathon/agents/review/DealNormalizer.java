package com.db.hackathon.agents.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Set;

@Slf4j
@Component
public class DealNormalizer {

    private static final Set<String> DEAL_FIELDS = Set.of(
            "version", "dealName", "currency", "department", "branch", "processingAreaCode",
            "classification", "agreementDate", "globalDealProposedCommitmentAmount",
            "dealAdminAgent", "dealBorrower", "interestPricingOptions", "facilityList");
    private static final Set<String> DEAL_LEAVES = Set.of(
            "dealName", "currency", "department", "branch", "processingAreaCode",
            "classification", "agreementDate", "globalDealProposedCommitmentAmount");
    private static final Set<String> FACILITY_FIELDS = Set.of(
            "version", "facilityName", "facilityType", "proposedCommitmentAmount",
            "closingCommitment", "agreementDate", "effectiveDate", "expiryDate",
            "finalMaturityDate", "facilityInterestPricingList", "facilitySublimit");
    private static final Set<String> FACILITY_LEAVES = Set.of(
            "facilityName", "facilityType", "proposedCommitmentAmount", "closingCommitment",
            "agreementDate", "effectiveDate", "expiryDate", "finalMaturityDate");
    private static final Set<String> EXTRACTED_FIELD_FIELDS =
            Set.of("value", "pageNumber", "confidence", "sourceText");

    public JsonNode normalize(JsonNode deal) {
        if (deal == null || deal.isNull()) {
            log.debug("Normalize skipped: deal is null");
            return null;
        }
        if (!deal.isObject()) {
            throw new IllegalArgumentException("Deal JSON must be an object");
        }

        ObjectNode normalized = (ObjectNode) deal.deepCopy();
        normalized.retain(DEAL_FIELDS);
        setDefaultVersion(normalized);
        fillMissing(normalized, DEAL_LEAVES);
        normalizeFields(normalized, DEAL_LEAVES.toArray(String[]::new));
        normalizeNamedObject(normalized, "dealAdminAgent", Set.of("agentName", "version"),
                "agentName");
        normalizeNamedObject(normalized, "dealBorrower", Set.of("borrowerName", "version"),
                "borrowerName");
        normalizeFieldArray(normalized, "interestPricingOptions",
                Set.of("pricingOption", "version"),
                "pricingOption");

        ArrayNode facilities = array(normalized, "facilityList");
        for (JsonNode item : facilities) {
            if (!item.isObject()) {
                throw new IllegalArgumentException("Each facility must be a JSON object");
            }
            ObjectNode facility = (ObjectNode) item;
            facility.retain(FACILITY_FIELDS);
            setDefaultVersion(facility);
            fillMissing(facility, FACILITY_LEAVES);
            normalizeFields(facility, FACILITY_LEAVES.toArray(String[]::new));
            normalizeFieldArray(facility, "facilityInterestPricingList",
                    Set.of("optionName", "rateBasis", "baseRate", "spread", "version"),
                    "optionName", "rateBasis", "baseRate", "spread");
            normalizeNamedObject(facility, "facilitySublimit",
                    Set.of("name", "effectiveDate", "version"), "name", "effectiveDate");
        }
        return normalized;
    }

    private void normalizeNamedObject(ObjectNode parent, String name, Set<String> allowedFields,
                                      String... extractedFields) {
        ObjectNode object = object(parent, name);
        object.retain(allowedFields);
        setDefaultVersion(object);
        fillMissing(object, allowedFields.stream().filter(field -> !"version".equals(field))
                .collect(java.util.stream.Collectors.toSet()));
        normalizeFields(object, extractedFields);
    }

    private void normalizeFieldArray(ObjectNode parent, String name, Set<String> allowedFields,
                                     String... extractedFields) {
        ArrayNode array = array(parent, name);
        for (JsonNode item : array) {
            if (!item.isObject()) {
                throw new IllegalArgumentException(name + " entries must be JSON objects");
            }
            ObjectNode object = (ObjectNode) item;
            object.retain(allowedFields);
            setDefaultVersion(object);
            fillMissing(object, allowedFields.stream().filter(field -> !"version".equals(field))
                    .collect(java.util.stream.Collectors.toSet()));
            normalizeFields(object, extractedFields);
        }
    }

    private void normalizeFields(ObjectNode object, String... names) {
        for (String name : names) {
            JsonNode value = object.get(name);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isValueNode()) {
                ObjectNode field = JsonNodeFactory.instance.objectNode();
                field.put("value", value.asText());
                field.put("confidence", 0.0);
                field.put("sourceText", "");
                object.set(name, field);
            } else if (value.isObject()) {
                ObjectNode field = (ObjectNode) value;
                field.retain(EXTRACTED_FIELD_FIELDS);
                fillMissing(field, EXTRACTED_FIELD_FIELDS);
            } else {
                throw new IllegalArgumentException(name + " must be an extracted field object or null");
            }
        }
    }

    private ObjectNode object(ObjectNode parent, String name) {
        JsonNode value = parent.get(name);
        if (value == null || value.isNull()) {
            return parent.putObject(name);
        }
        if (!value.isObject()) {
            throw new IllegalArgumentException(name + " must be a JSON object");
        }
        return (ObjectNode) value;
    }

    private ArrayNode array(ObjectNode parent, String name) {
        JsonNode value = parent.get(name);
        if (value == null || value.isNull()) {
            return parent.putArray(name);
        }
        if (!value.isArray()) {
            throw new IllegalArgumentException(name + " must be a JSON array");
        }
        return (ArrayNode) value;
    }

    private void setDefaultVersion(ObjectNode object) {
        if (!object.has("version")) {
            object.put("version", "1.0");
        }
    }

    private void fillMissing(ObjectNode object, Set<String> fields) {
        for (String field : fields) {
            if (!object.has(field)) {
                object.putNull(field);
            }
        }
    }
}
