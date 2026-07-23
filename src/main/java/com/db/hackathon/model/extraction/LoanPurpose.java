package com.db.hackathon.model.extraction;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoanPurpose {

    private ExtractedField loanPurposeCode;
}
