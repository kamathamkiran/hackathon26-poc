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
public class DealDataVerificationResult {

    private boolean valid;

    @Builder.Default
    private List<String> errors = new ArrayList<>();
}
