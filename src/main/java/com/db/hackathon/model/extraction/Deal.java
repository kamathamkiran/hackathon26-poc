package com.db.hackathon.model.extraction;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Deal {

    private ExtractedField dealName;

    private ExtractedField currency;

    private ExtractedField department;

    private ExtractedField branch;

    private ExtractedField processingAreaCode;

    private ExtractedField classification;

    private ExtractedField agreementDate;

    private ExtractedField globalDealProposedCommitmentAmount;

    private ExtractedField expenseCode;

    private DealAdminAgent dealAdminAgent;

    private ExtractedField dealBorrower;

    private ExtractedField borrowerIndicator;

    private List<InterestPricingOption> interestPricingOptions;

    private List<Facility> facilityList;
}
