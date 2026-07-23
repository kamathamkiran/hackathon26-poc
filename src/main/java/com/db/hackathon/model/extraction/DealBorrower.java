package com.db.hackathon.model.extraction;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealBorrower {

    private ExtractedField customerExternalId;

    private ExtractedField borrowerIndicator;
}
