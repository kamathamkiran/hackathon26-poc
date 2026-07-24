package com.db.hackathon.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DealBorrower {

    private ExtractedField customerExternalId;

    private ExtractedField borrowerIndicator;
}
