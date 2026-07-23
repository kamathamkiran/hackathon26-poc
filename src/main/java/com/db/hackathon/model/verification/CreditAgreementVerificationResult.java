package com.db.hackathon.model.verification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreditAgreementVerificationResult {

    private boolean verified;

    private int checkedFields;

    @Builder.Default
    private List<String> mismatches = new ArrayList<>();
}
