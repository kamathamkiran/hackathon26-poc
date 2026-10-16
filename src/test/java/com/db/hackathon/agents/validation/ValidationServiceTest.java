package com.db.hackathon.agents.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ValidationServiceTest {

    @Test
    void requiresAllFacilityPricingAttributesWhenPricingEntryExists() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidationRuleProvider ruleProvider = new ValidationRuleProvider(objectMapper);
        ValidationService validationService = new ValidationService(ruleProvider);
        JsonNode deal = objectMapper.readTree("""
                {
                  "facilityList": [{
                    "facilityInterestPricingList": [{}]
                  }],
                  "interestPricingOptions": []
                }
                """);

        var result = validationService.validate(deal);

        List<String> missingPricingFields = List.of(
                "facilityList[0].facilityInterestPricingList[0].optionName",
                "facilityList[0].facilityInterestPricingList[0].rateBasis",
                "facilityList[0].facilityInterestPricingList[0].baseRate",
                "facilityList[0].facilityInterestPricingList[0].spread"
        );
        for (String field : missingPricingFields) {
            assertTrue(result.getErrors().stream().anyMatch(issue -> field.equals(issue.getField())),
                    () -> "Expected a missing-field validation error for " + field);
        }
    }

    @Test
    void requiresAtLeastOnePricingOptionForEveryFacility() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidationService validationService =
                new ValidationService(new ValidationRuleProvider(objectMapper));
        JsonNode deal = objectMapper.readTree("""
                {
                  "facilityList": [{
                    "facilityInterestPricingList": []
                  }],
                  "interestPricingOptions": []
                }
                """);

        var result = validationService.validate(deal);

        assertTrue(result.getErrors().stream().anyMatch(issue ->
                "facilityList[0].facilityInterestPricingList".equals(issue.getField())
                        && issue.getMessage().contains("At least 1 Facility Pricing Option")));
    }

    @Test
    void validatesScalarFieldValuesFromJsonTree() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidationService validationService =
                new ValidationService(new ValidationRuleProvider(objectMapper));
        JsonNode deal = objectMapper.readTree("""
                {
                  "dealName": {"value": "Example", "pageNumber": 1, "confidence": 0.95},
                  "currency": {"value": "USD", "pageNumber": 1, "confidence": 0.95},
                  "facilityList": [{
                    "facilityType": {"value": "Term Loan", "pageNumber": 2},
                    "expiryDate": null,
                    "facilityInterestPricingList": []
                  }]
                }
                """);

        var result = validationService.validate(deal);

        assertTrue(result.getErrors().stream()
                .anyMatch(issue -> "facilityList[0].expiryDate".equals(issue.getField())));
    }

    @Test
    void requiresEveryConfiguredFieldExceptUiSuppliedFields() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidationService validationService =
                new ValidationService(new ValidationRuleProvider(objectMapper));
        JsonNode deal = objectMapper.readTree("""
                {
                  "dealName": null,
                  "currency": null,
                  "department": null,
                  "branch": null,
                  "processingAreaCode": null,
                  "classification": null,
                  "agreementDate": null,
                  "globalDealProposedCommitmentAmount": null,
                  "dealAdminAgent": {"agentName": null},
                  "dealBorrower": {"borrowerName": null},
                  "interestPricingOptions": [{"pricingOption": null}],
                  "facilityList": [{
                    "facilityName": null,
                    "facilityType": null,
                    "proposedCommitmentAmount": null,
                    "closingCommitment": null,
                    "agreementDate": null,
                    "effectiveDate": null,
                    "expiryDate": null,
                    "finalMaturityDate": null,
                    "facilityInterestPricingList": [{
                      "optionName": null,
                      "rateBasis": null,
                      "baseRate": null,
                      "spread": null
                    }],
                    "facilitySublimit": {"name": null, "effectiveDate": null}
                  }]
                }
                """);

        var result = validationService.validate(deal);
        List<String> errors = result.getErrors().stream()
                .map(issue -> issue.getField())
                .toList();

        List<String> requiredFields = List.of(
                "dealName", "currency", "classification", "agreementDate",
                "globalDealProposedCommitmentAmount", "dealAdminAgent.agentName",
                "dealBorrower.borrowerName", "interestPricingOptions[0].pricingOption",
                "facilityList[0].facilityName", "facilityList[0].facilityType",
                "facilityList[0].proposedCommitmentAmount",
                "facilityList[0].closingCommitment", "facilityList[0].agreementDate",
                "facilityList[0].effectiveDate", "facilityList[0].expiryDate",
                "facilityList[0].finalMaturityDate", "facilityList[0].facilitySublimit.name",
                "facilityList[0].facilitySublimit.effectiveDate",
                "facilityList[0].facilityInterestPricingList[0].optionName",
                "facilityList[0].facilityInterestPricingList[0].rateBasis",
                "facilityList[0].facilityInterestPricingList[0].baseRate",
                "facilityList[0].facilityInterestPricingList[0].spread");
        for (String field : requiredFields) {
            assertTrue(errors.contains(field), () -> "Expected mandatory-field error for " + field);
        }
        assertFalse(errors.contains("department"));
        assertFalse(errors.contains("branch"));
        assertFalse(errors.contains("processingAreaCode"));
    }

    @Test
    void calculatesMissingGlobalCommitmentFromAllFacilityAmounts() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidationService validationService =
                new ValidationService(new ValidationRuleProvider(objectMapper));
        JsonNode deal = objectMapper.readTree("""
                {
                  "currency": {"value": "USD"},
                  "globalDealProposedCommitmentAmount": null,
                  "facilityList": [
                    {
                      "proposedCommitmentAmount": {
                        "value": "1250000",
                        "pageNumber": 2,
                        "confidence": 0.96,
                        "sourceText": "Proposed Commitment: $1,250,000"
                      }
                    },
                    {
                      "proposedCommitmentAmount": {
                        "value": "750000.50",
                        "pageNumber": 3,
                        "confidence": 0.91,
                        "sourceText": "Proposed Commitment: $750,000.50"
                      }
                    }
                  ]
                }
                """);

        JsonNode calculated = validationService.calculateGlobalCommitmentIfMissing(deal);
        JsonNode global = calculated.path("globalDealProposedCommitmentAmount");

        assertEquals("2000000.5", global.path("value").asText());
        assertTrue(global.path("pageNumber").isNull());
        assertEquals(0.91, global.path("confidence").asDouble());
        assertEquals("Proposed Commitment: $1,250,000; Proposed Commitment: $750,000.50",
                global.path("sourceText").asText());
        assertTrue(deal.path("globalDealProposedCommitmentAmount").isNull());
    }

    @Test
    void doesNotOverwriteExplicitGlobalCommitmentOrCalculateWithoutCompleteEvidence()
            throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        ValidationService validationService =
                new ValidationService(new ValidationRuleProvider(objectMapper));
        JsonNode explicit = objectMapper.readTree("""
                {
                  "currency": {"value": "USD"},
                  "globalDealProposedCommitmentAmount": {"value": "900"},
                  "facilityList": []
                }
                """);
        JsonNode incomplete = objectMapper.readTree("""
                {
                  "currency": {"value": "USD"},
                  "globalDealProposedCommitmentAmount": null,
                  "facilityList": [{
                    "proposedCommitmentAmount": {"value": "100", "sourceText": "100"}
                  }]
                }
                """);

        assertSame(explicit, validationService.calculateGlobalCommitmentIfMissing(explicit));
        assertSame(incomplete, validationService.calculateGlobalCommitmentIfMissing(incomplete));
        assertTrue(incomplete.path("globalDealProposedCommitmentAmount").isNull());
    }
}
