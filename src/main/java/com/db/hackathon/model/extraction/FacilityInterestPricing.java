package com.db.hackathon.model.extraction;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FacilityInterestPricing {

    private ExtractedField optionName;

    private ExtractedField rateBasis;

    private ExtractedField baseRate;

    private ExtractedField spread;
}
