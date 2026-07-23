package com.db.hackathon.model.extraction;

import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Facility {

    private ExtractedField dealTrackingNumber;

    private ExtractedField facilityName;

    private ExtractedField facilityType;

    private ExtractedField proposedCommitmentAmount;

    private ExtractedField closingCommitment;

    private ExtractedField agreementDate;

    private ExtractedField effectiveDate;

    private ExtractedField expiryDate;

    private ExtractedField finalMaturityDate;

    private Risk risk;

    private LoanPurpose loanPurpose;

    private List<FacilityInterestPricing> facilityInterestPricingList;

    private ExtractedField facilitySublimit;

    private ExtractedField facilityRid;

    private ExtractedField globalNewAmount;
}
