package com.db.hackathon.adk.agent.validation;

import com.db.hackathon.enums.ValidationCode;
import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.model.extraction.ExtractedField;
import com.db.hackathon.model.extraction.Facility;
import com.db.hackathon.model.extraction.FacilityInterestPricing;
import com.db.hackathon.model.extraction.InterestPricingOption;
import com.db.hackathon.model.validation.FieldRule;
import com.db.hackathon.model.validation.ValidationIssue;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.model.validation.ValidationRuleSet;
import com.db.hackathon.model.validation.ValidationSeverity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ValidationService {

    private static final DateTimeFormatter ISO_DATE =
            DateTimeFormatter.ofPattern("uuuu-MM-dd").withResolverStyle(ResolverStyle.STRICT);

    private final ValidationRuleProvider ruleProvider;

    public ValidationResult validate(Deal deal) {
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        if (deal == null) {
            log.warn("Validation invoked with a null deal");
            errors.add(issue("deal", ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                    "No extracted deal data was available for validation", null));
            return ValidationResult.builder().valid(false).errors(errors).warnings(warnings).build();
        }

        applyDeclarativeRules(deal, errors, warnings);
        applyBusinessRules(deal, errors, warnings);

        boolean valid = errors.isEmpty();
        log.info("Validation completed: valid={}, errors={}, warnings={}",
                valid, errors.size(), warnings.size());
        if (!errors.isEmpty()) {
            log.debug("Validation errors: {}", errors);
        }

        return ValidationResult.builder()
                .valid(valid)
                .errors(errors)
                .warnings(warnings)
                .build();
    }

    private void applyDeclarativeRules(Deal deal, List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        ValidationRuleSet rules = ruleProvider.ruleSet();

        applySection(rules.section("deal"), deal, "", errors, warnings);
        applySection(rules.section("dealAdminAgent"), deal.getDealAdminAgent(),
                "dealAdminAgent.", errors, warnings);
        applySection(rules.section("dealAdminServicingGroup"),
                deal.getDealAdminAgent() == null ? null : deal.getDealAdminAgent().getDealAdminServicingGroup(),
                "dealAdminAgent.dealAdminServicingGroup.", errors, warnings);
        applySection(rules.section("dealBorrower"), deal.getDealBorrower(),
                "dealBorrower.", errors, warnings);

        List<InterestPricingOption> options = deal.getInterestPricingOptions();
        if (options != null) {
            for (int k = 0; k < options.size(); k++) {
                applySection(rules.section("interestPricingOption"), options.get(k),
                        "interestPricingOptions[" + k + "].", errors, warnings);
            }
        }

        List<Facility> facilities = deal.getFacilityList();
        if (facilities != null) {
            log.debug("Applying declarative rules to {} facility/facilities", facilities.size());
            for (int i = 0; i < facilities.size(); i++) {
                Facility facility = facilities.get(i);
                String prefix = "facilityList[" + i + "].";

                applySection(rules.section("facility"), facility, prefix, errors, warnings);
                applySection(rules.section("risk"), facility.getRisk(),
                        prefix + "risk.", errors, warnings);
                applySection(rules.section("loanPurpose"), facility.getLoanPurpose(),
                        prefix + "loanPurpose.", errors, warnings);

                List<FacilityInterestPricing> pricingList = facility.getFacilityInterestPricingList();
                if (pricingList != null) {
                    for (int j = 0; j < pricingList.size(); j++) {
                        applySection(rules.section("pricing"), pricingList.get(j),
                                prefix + "facilityInterestPricingList[" + j + "].", errors, warnings);
                    }
                }
            }
        }
    }

    private void applySection(List<FieldRule> rules, Object target, String prefix,
                              List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        if (rules == null) {
            return;
        }
        for (FieldRule rule : rules) {
            ExtractedField field = readField(target, rule.getField());
            applyRule(rule, field, prefix + rule.getField(), errors, warnings);
        }
    }

    private void applyRule(FieldRule rule, ExtractedField field, String path,
                           List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        String value = text(field);

        if (value == null) {
            if (rule.isMandatory()) {
                errors.add(issue(path, ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                        rule.getLabel() + " is mandatory", page(field)));
            }
            return;
        }

        switch (rule.getType()) {
            case CURRENCY -> validateCurrency(rule, value, path, field, errors);
            case DATE -> validateDate(rule, field, value, path, errors);
            case AMOUNT -> validateAmount(rule, field, value, path, errors);
            case STRING -> { }
        }

        if (rule.getMaxLength() != null && value.length() > rule.getMaxLength()) {
            errors.add(issue(path, ValidationCode.OUT_OF_RANGE, ValidationSeverity.ERROR,
                    rule.getLabel() + " exceeds maximum length of " + rule.getMaxLength(), page(field)));
        }

        lowConfidence(rule, field, path, warnings);
    }

    private void validateCurrency(FieldRule rule, String value, String path,
                                  ExtractedField field, List<ValidationIssue> errors) {
        List<String> allowed = rule.getAllowedValues();
        if (allowed != null && !allowed.isEmpty()
                && allowed.stream().noneMatch(v -> v.equalsIgnoreCase(value))) {
            errors.add(issue(path, ValidationCode.INVALID_CURRENCY, ValidationSeverity.ERROR,
                    rule.getLabel() + " '" + value + "' is not allowed. Allowed: " + allowed,
                    page(field)));
        }
    }

    private void validateDate(FieldRule rule, ExtractedField field, String value, String path,
                              List<ValidationIssue> errors) {
        if (parseIso(field) == null) {
            errors.add(issue(path, ValidationCode.INVALID_DATE, ValidationSeverity.ERROR,
                    rule.getLabel() + " '" + value + "' must be an ISO date (yyyy-MM-dd)", page(field)));
        }
    }

    private void validateAmount(FieldRule rule, ExtractedField field, String value, String path,
                                List<ValidationIssue> errors) {
        BigDecimal amount = parseAmount(field);
        if (amount == null) {
            errors.add(issue(path, ValidationCode.INVALID_AMOUNT, ValidationSeverity.ERROR,
                    rule.getLabel() + " '" + value + "' must be numeric", page(field)));
            return;
        }
        if (rule.isPositive() && amount.signum() <= 0) {
            errors.add(issue(path, ValidationCode.OUT_OF_RANGE, ValidationSeverity.ERROR,
                    rule.getLabel() + " must be greater than 0", page(field)));
        } else if (rule.isNonNegative() && amount.signum() < 0) {
            errors.add(issue(path, ValidationCode.OUT_OF_RANGE, ValidationSeverity.ERROR,
                    rule.getLabel() + " must be greater than or equal to 0", page(field)));
        }
    }

    private void lowConfidence(FieldRule rule, ExtractedField field, String path,
                               List<ValidationIssue> warnings) {
        double threshold = ruleProvider.ruleSet().getLowConfidenceThreshold();
        if (field != null && field.getConfidence() != null && field.getConfidence() < threshold) {
            warnings.add(issue(path, ValidationCode.LOW_CONFIDENCE, ValidationSeverity.WARNING,
                    rule.getLabel() + " has low extraction confidence ("
                            + field.getConfidence() + "); please verify", page(field)));
        }
    }

    private void applyBusinessRules(Deal deal, List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        requireAtLeastOneFacility(deal, errors);
        validateCommitmentTotal(deal, warnings);

        List<Facility> facilities = deal.getFacilityList();
        if (facilities == null) {
            return;
        }
        for (int i = 0; i < facilities.size(); i++) {
            Facility facility = facilities.get(i);
            String prefix = "facilityList[" + i + "]";

            validateDateOrder(prefix, facility, errors);
            validateClosingWithinProposed(prefix, facility, errors);
            validateTermLoanExpiry(prefix, facility, errors);
            validatePricingRateBasis(prefix, facility, errors);
        }

        validateCrossCurrency(deal, warnings);
    }

    private void requireAtLeastOneFacility(Deal deal, List<ValidationIssue> errors) {
        if (deal.getFacilityList() == null || deal.getFacilityList().isEmpty()) {
            errors.add(issue("facilityList", ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                    "At least one Facility is required", null));
        }
    }

    private void validateCommitmentTotal(Deal deal, List<ValidationIssue> warnings) {
        BigDecimal global = parseAmount(deal.getGlobalDealProposedCommitmentAmount());
        if (global == null || deal.getFacilityList() == null || deal.getFacilityList().isEmpty()) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Facility facility : deal.getFacilityList()) {
            BigDecimal amount = parseAmount(facility.getProposedCommitmentAmount());
            if (amount == null) {
                return;
            }
            total = total.add(amount);
        }
        if (global.compareTo(total) != 0) {
            warnings.add(issue("globalDealProposedCommitmentAmount", ValidationCode.BUSINESS_RULE,
                    ValidationSeverity.WARNING,
                    "Global Proposed Commitment (" + global + ") does not equal the sum of facility "
                            + "proposed commitments (" + total + ")", null));
        }
    }

    private void validateDateOrder(String prefix, Facility facility, List<ValidationIssue> errors) {
        compareOrder(prefix, "agreementDate", facility.getAgreementDate(),
                "effectiveDate", facility.getEffectiveDate(), errors);
        compareOrder(prefix, "effectiveDate", facility.getEffectiveDate(),
                "expiryDate", facility.getExpiryDate(), errors);
        compareOrder(prefix, "expiryDate", facility.getExpiryDate(),
                "finalMaturityDate", facility.getFinalMaturityDate(), errors);
    }

    private void compareOrder(String prefix, String earlierName, ExtractedField earlier,
                              String laterName, ExtractedField later, List<ValidationIssue> errors) {
        LocalDate earlierDate = parseIso(earlier);
        LocalDate laterDate = parseIso(later);
        if (earlierDate != null && laterDate != null && laterDate.isBefore(earlierDate)) {
            errors.add(issue(prefix + "." + laterName, ValidationCode.OUT_OF_RANGE,
                    ValidationSeverity.ERROR,
                    laterName + " must be on or after " + earlierName, page(later)));
        }
    }

    private void validateClosingWithinProposed(String prefix, Facility facility, List<ValidationIssue> errors) {
        BigDecimal closing = parseAmount(facility.getClosingCommitment());
        BigDecimal proposed = parseAmount(facility.getProposedCommitmentAmount());
        if (closing != null && proposed != null && closing.compareTo(proposed) > 0) {
            errors.add(issue(prefix + ".closingCommitment", ValidationCode.BUSINESS_RULE,
                    ValidationSeverity.ERROR,
                    "Closing Commitment must not exceed Proposed Commitment",
                    page(facility.getClosingCommitment())));
        }
    }

    private void validateTermLoanExpiry(String prefix, Facility facility, List<ValidationIssue> errors) {
        String type = text(facility.getFacilityType());
        if (type != null && type.toLowerCase().contains("term loan")
                && text(facility.getExpiryDate()) == null) {
            errors.add(issue(prefix + ".expiryDate", ValidationCode.BUSINESS_RULE,
                    ValidationSeverity.ERROR,
                    "Expiry Date is required for a Term Loan facility",
                    page(facility.getFacilityType())));
        }
    }

    private void validatePricingRateBasis(String prefix, Facility facility, List<ValidationIssue> errors) {
        List<FacilityInterestPricing> pricingList = facility.getFacilityInterestPricingList();
        if (pricingList == null) {
            return;
        }
        for (int j = 0; j < pricingList.size(); j++) {
            FacilityInterestPricing pricing = pricingList.get(j);
            if (text(pricing.getBaseRate()) != null && text(pricing.getRateBasis()) == null) {
                errors.add(issue(prefix + ".facilityInterestPricingList[" + j + "].rateBasis",
                        ValidationCode.BUSINESS_RULE, ValidationSeverity.ERROR,
                        "Rate Basis is required when an indexed Base Rate is present",
                        page(pricing.getBaseRate())));
            }
        }
    }

    private void validateCrossCurrency(Deal deal, List<ValidationIssue> warnings) {
        String dealCurrency = text(deal.getCurrency());
        if (dealCurrency == null || deal.getFacilityList() == null) {
            return;
        }
        if (!"USD".equalsIgnoreCase(dealCurrency)) {
            warnings.add(issue("currency", ValidationCode.BUSINESS_RULE, ValidationSeverity.WARNING,
                    "Deal currency is " + dealCurrency
                            + "; confirm each facility currency matches the deal currency", null));
        }
    }

    private ValidationIssue issue(String field, ValidationCode code, ValidationSeverity severity,
                                  String message, Integer pageNumber) {
        return ValidationIssue.builder()
                .field(field)
                .code(code)
                .severity(severity)
                .message(message)
                .pageNumber(pageNumber)
                .build();
    }

    private ExtractedField readField(Object target, String property) {
        if (target == null || property == null || property.isEmpty()) {
            return null;
        }
        try {
            String getter = "get" + Character.toUpperCase(property.charAt(0)) + property.substring(1);
            Method method = target.getClass().getMethod(getter);
            Object value = method.invoke(target);
            return value instanceof ExtractedField extracted ? extracted : null;
        } catch (ReflectiveOperationException ex) {
            log.debug("Could not read field '{}' from {}: {}", property,
                    target.getClass().getSimpleName(), ex.getMessage());
            return null;
        }
    }

    private String text(ExtractedField field) {
        if (field == null || field.getValue() == null || field.getValue().isBlank()) {
            return null;
        }
        return field.getValue().trim();
    }

    private Integer page(ExtractedField field) {
        return field == null ? null : field.getPageNumber();
    }

    private LocalDate parseIso(ExtractedField field) {
        String value = text(field);
        if (value == null) {
            return null;
        }
        try {
            return LocalDate.parse(value, ISO_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    private BigDecimal parseAmount(ExtractedField field) {
        String value = text(field);
        if (value == null) {
            return null;
        }
        try {
            return new BigDecimal(value.replaceAll("[,$\\s]", ""));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
