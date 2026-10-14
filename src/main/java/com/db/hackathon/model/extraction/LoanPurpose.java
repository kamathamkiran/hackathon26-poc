package com.db.hackathon.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LoanPurpose {

    @Builder.Default
    private String version = "1.0";

    private ExtractedField loanPurposeCode;
}
