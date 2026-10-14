package com.db.hackathon.model.extraction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DealSchemaTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deserializesAndPreservesNestedSchemaAndVersionFields() throws Exception {
        String json = """
                {
                  "version": "1.0",
                  "dealBorrower": {
                    "customerExternalId": {
                      "value": "BORROWER-1",
                      "pageNumber": 2,
                      "confidence": 0.98,
                      "sourceText": "Borrower ID BORROWER-1"
                    },
                    "borrowerIndicator": null,
                    "version": "1.0"
                  },
                  "facilityList": [{
                    "facilitySublimit": {
                      "name": null,
                      "effectiveDate": null,
                      "facilitySublimitRisk": {
                        "riskTypeCode": null,
                        "version": "1.0"
                      },
                      "version": "1.0"
                    },
                    "facilityRid": null,
                    "version": "1.0",
                    "globalNewAmount": null
                  }]
                }
                """;

        Deal deal = objectMapper.readValue(json, Deal.class);

        assertEquals("BORROWER-1", deal.getDealBorrower().getCustomerExternalId().getValue());
        assertEquals("1.0", deal.getDealBorrower().getVersion());
        assertEquals("1.0", deal.getVersion());
        assertNotNull(deal.getFacilityList().get(0).getFacilitySublimit().getFacilitySublimitRisk());
        assertEquals("1.0", deal.getFacilityList().get(0).getFacilitySublimit().getVersion());

        JsonNode serialized = objectMapper.readTree(objectMapper.writeValueAsString(deal));
        assertEquals("1.0", serialized.at("/facilityList/0/version").asText());
        assertEquals("1.0", serialized.at(
                "/facilityList/0/facilitySublimit/facilitySublimitRisk/version").asText());
        assertTrue(serialized.at("/facilityList/0/facilityRid").isNull());
    }

    @Test
    void deserializesScalarPricingFieldWithoutInventingExtractionEvidence() throws Exception {
        String json = """
                {
                  "facilityList": [{
                    "facilityInterestPricingList": [{
                      "optionName": "Pricing Level I"
                    }]
                  }]
                }
                """;

        Deal deal = objectMapper.readValue(json, Deal.class);
        ExtractedField optionName = deal.getFacilityList().get(0)
                .getFacilityInterestPricingList().get(0).getOptionName();

        assertEquals("Pricing Level I", optionName.getValue());
        assertEquals(0.0, optionName.getConfidence());
        assertNull(optionName.getPageNumber());
        assertEquals("", optionName.getSourceText());
    }
}
