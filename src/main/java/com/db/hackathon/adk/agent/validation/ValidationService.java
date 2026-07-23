package com.db.hackathon.adk.agent.validation;

import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.model.extraction.ExtractedField;
import com.db.hackathon.model.extraction.Facility;
import com.db.hackathon.model.validation.ValidationMessage;
import com.db.hackathon.model.validation.ValidationResult;
import com.google.api.pathtemplate.ValidationException;
import org.springframework.stereotype.Component;

@Component
public class ValidationService {

    void validateMandatoryFields(Deal deal) {

        validate("Deal Name", deal.getDealName());

        validate("Currency", deal.getCurrency());

        validate("Agreement Date", deal.getAgreementDate());

        validate("Global Commitment Amount",
                deal.getGlobalDealProposedCommitmentAmount());

        if (deal.getDealBorrower() == null) {
            throw new ValidationException("Deal Borrower is mandatory.");
        }

        validate("Borrower",
                deal.getDealBorrower().getCustomerExternalId());

        if (deal.getDealAdminAgent() == null) {
            throw new ValidationException("Deal Admin Agent is mandatory.");
        }

        validate("Admin Agent",
                deal.getDealAdminAgent().getCustomerExternalId());

        if (deal.getFacilityList() == null ||
                deal.getFacilityList().isEmpty()) {

            throw new ValidationException(
                    "At least one Facility is required.");
        }

        for (int i = 0; i < deal.getFacilityList().size(); i++) {

            Facility facility =
                    deal.getFacilityList().get(i);

            validate(
                    "Facility[" + i + "] Name",
                    facility.getFacilityName());

            validate(
                    "Facility[" + i + "] Type",
                    facility.getFacilityType());

            validate(
                    "Facility[" + i + "] Commitment Amount",
                    facility.getProposedCommitmentAmount());
        }

        if (deal.getInterestPricingOptions() != null) {

            for (int i = 0; i < deal.getInterestPricingOptions().size(); i++) {

                validate(
                        "Interest Pricing[" + i + "]",
                        deal.getInterestPricingOptions()
                                .get(i)
                                .getPricingOption());
            }
        }
    }

    private void validate(
            String fieldName,
            ExtractedField field) {

        if (field == null
                || field.getValue() == null
                || field.getValue().isBlank()) {

            throw new ValidationException(
                    fieldName + " is mandatory.");
        }
    }

}
