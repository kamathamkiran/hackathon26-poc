package com.db.hackathon.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FacilityInterestPricing {

    private ExtractedField optionName;

    private ExtractedField rateBasis;

    private ExtractedField baseRate;

    private ExtractedField spread;
}
