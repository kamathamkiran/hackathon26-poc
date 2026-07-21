package com.db.hackathon.model.aggrement;

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
        switch (field) {
            case DEAL_NAME:
                return dealName;
            case BORROWER:
                return borrower;
            case FACILITY_AMOUNT:
                return facilityAmount;
            case CURRENCY:
                return currency;
            case INTEREST_RATE:
                return interestRate;
            case EFFECTIVE_DATE:
                return effectiveDate;
            case MATURITY_DATE:
                return maturityDate;
            case FACILITY_TYPE:
                return facilityType;
            case ARRANGER:
                return arranger;
            case GOVERNING_LAW:
                return governingLaw;
            default:
                throw new IllegalArgumentException("Unknown field: " + field);
        }
    }
}
