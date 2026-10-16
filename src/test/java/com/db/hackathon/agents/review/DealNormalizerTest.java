package com.db.hackathon.agents.review;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DealNormalizer normalizer = new DealNormalizer();

    @Test
    void normalizesJsonTreeAndDropsFieldsOutsideTheDealSchema() throws Exception {
        String json = """
                {
                  "expenseCode": null,
                  "version": "1.0",
                  "dealAdminAgent": {
                    "agentName": {
                      "value": "Example Bank",
                      "pageNumber": 1,
                      "confidence": 0.98,
                      "sourceText": "Example Bank"
                    },
                    "dealAdminServicingGroup": {
                      "profileType": null,
                      "version": "1.0"
                    },
                    "version": "1.0"
                  },
                  "dealBorrower": {
                    "borrowerName": {
                      "value": "BORROWER-1",
                      "pageNumber": 2,
                      "confidence": 0.98,
                      "sourceText": "Borrower ID BORROWER-1"
                    },
                    "version": "1.0"
                  },
                  "interestPricingOptions": [],
                  "facilityList": [{
                    "risk": {
                      "riskTypeCode": null,
                      "version": "1.0"
                    },
                    "loanPurpose": {
                      "loanPurposeCode": null,
                      "version": "1.0"
                    },
                    "facilitySublimit": {
                      "name": null,
                      "effectiveDate": null,
                      "facilitySublimitRisk": {
                        "riskTypeCode": null,
                        "version": "1.0"
                      },
                      "version": "1.0"
                    },
                    "version": "1.0",
                    "globalNewAmount": {
                      "value": "250000",
                      "pageNumber": 3,
                      "confidence": 0.9,
                      "sourceText": "Global new amount: 250,000"
                    }
                  }]
                }
                """;

        JsonNode deal = normalizer.normalize(objectMapper.readTree(json));

        assertEquals("Example Bank", deal.at("/dealAdminAgent/agentName/value").asText());
        assertEquals("BORROWER-1", deal.at("/dealBorrower/borrowerName/value").asText());
        assertEquals("1.0", deal.at("/dealBorrower/version").asText());
        assertEquals("1.0", deal.at("/version").asText());
        assertEquals("1.0", deal.at("/facilityList/0/facilitySublimit/version").asText());
        assertTrue(deal.at("/expenseCode").isMissingNode());
        assertTrue(deal.at("/dealBorrower/borrowerIndicator").isMissingNode());
        assertTrue(deal.at("/dealAdminAgent/dealAdminServicingGroup").isMissingNode());
        assertTrue(deal.at("/facilityList/0/risk").isMissingNode());
        assertTrue(deal.at("/facilityList/0/loanPurpose").isMissingNode());
        assertTrue(deal.at("/facilityList/0/facilitySublimit/facilitySublimitRisk").isMissingNode());
        assertTrue(deal.at("/facilityList/0/globalNewAmount").isMissingNode());
        assertTrue(deal.at("/dealAdminAgent/customerExternalId").isMissingNode());
        assertTrue(deal.at("/dealBorrower/customerExternalId").isMissingNode());
        assertTrue(deal.at("/facilityList/0/facilityRid").isMissingNode());
    }

    @Test
    void normalizesScalarExtractionLeavesWithoutInventingEvidence() throws Exception {
        JsonNode deal = normalizer.normalize(objectMapper.readTree("""
                {
                  "facilityList": [{
                    "facilityInterestPricingList": [{
                      "optionName": "Pricing Level I"
                    }]
                  }]
                }
                """));

        JsonNode optionName = deal.at(
                "/facilityList/0/facilityInterestPricingList/0/optionName");
        assertEquals("Pricing Level I", optionName.path("value").asText());
        assertEquals(0.0, optionName.path("confidence").asDouble());
        assertTrue(optionName.path("pageNumber").isMissingNode());
        assertEquals("", optionName.path("sourceText").asText());
    }

    @Test
    void normalizesMissingListsAndNestedObjects() throws Exception {
        JsonNode deal = normalizer.normalize(objectMapper.readTree("""
                {"facilityList": [{}]}
                """));

        assertEquals(0, deal.path("interestPricingOptions").size());
        assertEquals(1, deal.path("facilityList").size());
        assertEquals(0, deal.at("/facilityList/0/facilityInterestPricingList").size());
        assertTrue(deal.at("/facilityList/0/facilitySublimit").isObject());
    }

    @Test
    void leavesNullDealNull() {
        assertNull(normalizer.normalize(null));
    }
}
