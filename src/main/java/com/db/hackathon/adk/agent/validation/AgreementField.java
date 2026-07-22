package com.db.hackathon.adk.agent.validation;

import lombok.Getter;

@Getter
public enum AgreementField {

    DEAL_NAME(true),
    BORROWER(true),
    FACILITY_AMOUNT(true),
    CURRENCY(true),
    EFFECTIVE_DATE(true),
    MATURITY_DATE(false),
    INTEREST_RATE(false),
    ARRANGER(false),
    GOVERNING_LAW(false),
    FACILITY_TYPE(true);

    private final boolean mandatory;

    AgreementField(boolean mandatory) {
        this.mandatory = mandatory;
    }

}
