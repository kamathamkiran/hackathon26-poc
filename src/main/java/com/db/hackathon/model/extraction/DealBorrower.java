package com.db.hackathon.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DealBorrower {

    @Builder.Default
    private String version = "1.0";

    private ExtractedField customerExternalId;

    private ExtractedField borrowerIndicator;
}
