package com.db.hackathon.model.extraction;

import com.db.hackathon.enums.AgreementField;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Agreement {

    private ExtractedField dealName;

    private ExtractedField borrower;

    private ExtractedField facilityAmount;

    private ExtractedField currency;

    private ExtractedField interestRate;

    private ExtractedField effectiveDate;

    private ExtractedField maturityDate;

    private ExtractedField facilityType;

    private ExtractedField arranger;

    private ExtractedField governingLaw;

    public ExtractedField getField(AgreementField field) {
        return switch (field) {
            case DEAL_NAME -> dealName;
            case BORROWER -> borrower;
            case FACILITY_AMOUNT -> facilityAmount;
            case CURRENCY -> currency;
            case INTEREST_RATE -> interestRate;
            case EFFECTIVE_DATE -> effectiveDate;
            case MATURITY_DATE -> maturityDate;
            case FACILITY_TYPE -> facilityType;
            case ARRANGER -> arranger;
            case GOVERNING_LAW -> governingLaw;
            default -> throw new IllegalArgumentException("Unknown field: " + field);
        };
    }
}
