package com.db.hackathon.agents.validation;

import com.db.hackathon.enums.ValidationCode;
import com.db.hackathon.model.validation.FieldRule;
import com.db.hackathon.model.validation.FieldType;
import com.db.hackathon.model.validation.ValidationIssue;
import com.db.hackathon.model.validation.ValidationResult;
import com.db.hackathon.model.validation.ValidationRuleSet;
import com.db.hackathon.model.validation.ValidationSeverity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public JsonNode calculateGlobalCommitmentIfMissing(JsonNode deal) {
        if (deal == null || !deal.isObject()) {
            return deal;
        }

        JsonNode currentGlobal = deal.get("globalDealProposedCommitmentAmount");
        if (text(currentGlobal) != null) {
            return deal;
        }

        String currency = text(deal.get("currency"));
        JsonNode facilities = array(deal, "facilityList");
        if (currency == null || facilities == null || facilities.isEmpty()) {
            return deal;
        }

        BigDecimal total = BigDecimal.ZERO;
        double confidence = 1.0;
        Integer pageNumber = null;
        boolean pagesMatch = true;
        StringBuilder sourceText = new StringBuilder();
        for (JsonNode facility : facilities) {
            JsonNode amount = facility.get("proposedCommitmentAmount");
            BigDecimal value = parseAmount(amount);
            JsonNode quote = amount != null && amount.isObject() ? amount.get("sourceText") : null;
            JsonNode amountConfidence = amount != null && amount.isObject()
                    ? amount.get("confidence") : null;
            JsonNode amountPage = amount != null && amount.isObject()
                    ? amount.get("pageNumber") : null;
            if (value == null || quote == null || !quote.isTextual() || quote.asText().isBlank()
                    || amountConfidence == null || !amountConfidence.isNumber()
                    || amountConfidence.doubleValue() < 0.0 || amountConfidence.doubleValue() > 1.0) {
                log.debug("Cannot calculate global commitment: facility amount/evidence is incomplete");
                return deal;
            }

            total = total.add(value);
            confidence = Math.min(confidence, amountConfidence.doubleValue());
            Integer facilityPage = amountPage != null && amountPage.canConvertToInt()
                    ? amountPage.intValue() : null;
            if (sourceText.isEmpty()) {
                pageNumber = facilityPage;
            } else if (facilityPage == null || !facilityPage.equals(pageNumber)) {
                pagesMatch = false;
                sourceText.append("; ");
            } else {
                sourceText.append("; ");
            }
            sourceText.append(quote.asText());
        }

        ObjectNode calculated = ((ObjectNode) deal).deepCopy();
        ObjectNode global = calculated.putObject("globalDealProposedCommitmentAmount");
        global.put("value", total.stripTrailingZeros().toPlainString());
        if (pagesMatch && pageNumber != null) {
            global.put("pageNumber", pageNumber);
        } else {
            global.putNull("pageNumber");
        }
        global.put("confidence", confidence);
        global.put("sourceText", sourceText.toString());
        log.info("Calculated missing global proposed commitment as {} {}", total, currency);
        return calculated;
    }

    public ValidationResult validate(JsonNode deal) {
        List<ValidationIssue> errors = new ArrayList<>();
        List<ValidationIssue> warnings = new ArrayList<>();

        if (deal == null || deal.isNull()) {
            log.warn("Validation invoked with a null deal");
            errors.add(issue("deal", ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                    "No extracted deal data was available for validation", null));
            return ValidationResult.builder().valid(false).errors(errors).warnings(warnings).build();
        }
        if (!deal.isObject()) {
            throw new IllegalArgumentException("Deal JSON must be an object");
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

    private void applyDeclarativeRules(JsonNode deal, List<ValidationIssue> errors,
                                       List<ValidationIssue> warnings) {
        ValidationRuleSet rules = ruleProvider.ruleSet();

        applySection(rules.section("deal"), deal, "", errors, warnings);
        applySection(rules.section("dealBorrower"), deal.get("dealBorrower"),
                "dealBorrower.", errors, warnings);
        applySection(rules.section("dealAdminAgent"), deal.get("dealAdminAgent"),
                "dealAdminAgent.", errors, warnings);

        JsonNode options = array(deal, "interestPricingOptions");
        if (options != null) {
            for (int i = 0; i < options.size(); i++) {
                applySection(rules.section("interestPricingOption"), options.get(i),
                        "interestPricingOptions[" + i + "].", errors, warnings);
            }
        }

        JsonNode facilities = array(deal, "facilityList");
        if (facilities != null) {
            log.debug("Applying declarative rules to {} facility/facilities", facilities.size());
            for (int i = 0; i < facilities.size(); i++) {
                JsonNode facility = facilities.get(i);
                String prefix = "facilityList[" + i + "].";
                applySection(rules.section("facility"), facility, prefix, errors, warnings);
                applySection(rules.section("facilitySublimit"), facility.get("facilitySublimit"),
                        prefix + "facilitySublimit.", errors, warnings);

                JsonNode pricing = array(facility, "facilityInterestPricingList");
                if (pricing != null) {
                    for (int j = 0; j < pricing.size(); j++) {
                        applySection(rules.section("pricing"), pricing.get(j),
                                prefix + "facilityInterestPricingList[" + j + "].",
                                errors, warnings);
                    }
                }
            }
        }
    }

    private void applySection(List<FieldRule> rules, JsonNode target, String prefix,
                              List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        if (rules == null) {
            return;
        }
        for (FieldRule rule : rules) {
            JsonNode field = target == null ? null : target.get(rule.getField());
            applyRule(rule, field, prefix + rule.getField(), errors, warnings);
        }
    }

    private void applyRule(FieldRule rule, JsonNode field, String path,
                           List<ValidationIssue> errors, List<ValidationIssue> warnings) {
        if (rule.getType() == FieldType.ARRAY) {
            validateArray(rule, field, path, errors);
            return;
        }

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
            case STRING, ARRAY -> { }
        }

        if (rule.getMaxLength() != null && value.length() > rule.getMaxLength()) {
            errors.add(issue(path, ValidationCode.OUT_OF_RANGE, ValidationSeverity.ERROR,
                    rule.getLabel() + " exceeds maximum length of " + rule.getMaxLength(),
                    page(field)));
        }

        lowConfidence(rule, field, path, warnings);
    }

    private void validateArray(FieldRule rule, JsonNode field, String path,
                               List<ValidationIssue> errors) {
        if (field == null || field.isNull()) {
            if (rule.isMandatory()) {
                errors.add(issue(path, ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                        rule.getLabel() + " is mandatory", null));
            }
            return;
        }
        if (!field.isArray()) {
            errors.add(issue(path, ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                    rule.getLabel() + " must be a JSON array", null));
            return;
        }
        if (rule.getMinItems() != null && field.size() < rule.getMinItems()) {
            errors.add(issue(path, ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                    "At least " + rule.getMinItems() + " " + rule.getLabel()
                            + " is required for this facility", null));
        }
    }

    private void validateCurrency(FieldRule rule, String value, String path, JsonNode field,
                                  List<ValidationIssue> errors) {
        List<String> allowed = rule.getAllowedValues();
        if (allowed != null && !allowed.isEmpty()
                && allowed.stream().noneMatch(v -> v.equalsIgnoreCase(value))) {
            errors.add(issue(path, ValidationCode.INVALID_CURRENCY, ValidationSeverity.ERROR,
                    rule.getLabel() + " '" + value + "' is not allowed. Allowed: " + allowed,
                    page(field)));
        }
    }

    private void validateDate(FieldRule rule, JsonNode field, String value, String path,
                              List<ValidationIssue> errors) {
        if (parseIso(field) == null) {
            errors.add(issue(path, ValidationCode.INVALID_DATE, ValidationSeverity.ERROR,
                    rule.getLabel() + " '" + value + "' must be an ISO date (yyyy-MM-dd)",
                    page(field)));
        }
    }

    private void validateAmount(FieldRule rule, JsonNode field, String value, String path,
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

    private void lowConfidence(FieldRule rule, JsonNode field, String path,
                               List<ValidationIssue> warnings) {
        JsonNode confidence = field != null && field.isObject() ? field.get("confidence") : null;
        double threshold = ruleProvider.ruleSet().getLowConfidenceThreshold();
        if (confidence != null && confidence.isNumber() && confidence.doubleValue() < threshold) {
            warnings.add(issue(path, ValidationCode.LOW_CONFIDENCE, ValidationSeverity.WARNING,
                    rule.getLabel() + " has low extraction confidence ("
                            + confidence.doubleValue() + "); please verify", page(field)));
        }
    }

    private void applyBusinessRules(JsonNode deal, List<ValidationIssue> errors,
                                    List<ValidationIssue> warnings) {
        JsonNode facilities = array(deal, "facilityList");
        if (facilities == null || facilities.size() == 0) {
            errors.add(issue("facilityList", ValidationCode.MISSING_FIELD, ValidationSeverity.ERROR,
                    "At least one Facility is required", null));
        }

        if (facilities == null) {
            return;
        }
        validateCommitmentTotal(deal, facilities, warnings);
        for (int i = 0; i < facilities.size(); i++) {
            JsonNode facility = facilities.get(i);
            String prefix = "facilityList[" + i + "]";

            validateDateOrder(prefix, facility, errors);
            validateClosingWithinProposed(prefix, facility, errors);
            validateTermLoanExpiry(prefix, facility, errors);
            validatePricingRateBasis(prefix, facility, errors);
        }
        validateCrossCurrency(deal, warnings);
    }

    private void validateCommitmentTotal(JsonNode deal, JsonNode facilities,
                                         List<ValidationIssue> warnings) {
        BigDecimal global = parseAmount(deal.get("globalDealProposedCommitmentAmount"));
        if (global == null || facilities.isEmpty()) {
            return;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (JsonNode facility : facilities) {
            BigDecimal amount = parseAmount(facility.get("proposedCommitmentAmount"));
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

    private void validateDateOrder(String prefix, JsonNode facility, List<ValidationIssue> errors) {
        compareOrder(prefix, "agreementDate", facility.get("agreementDate"),
                "effectiveDate", facility.get("effectiveDate"), errors);
        compareOrder(prefix, "effectiveDate", facility.get("effectiveDate"),
                "expiryDate", facility.get("expiryDate"), errors);
        compareOrder(prefix, "expiryDate", facility.get("expiryDate"),
                "finalMaturityDate", facility.get("finalMaturityDate"), errors);
    }

    private void compareOrder(String prefix, String earlierName, JsonNode earlier,
                              String laterName, JsonNode later, List<ValidationIssue> errors) {
        LocalDate earlierDate = parseIso(earlier);
        LocalDate laterDate = parseIso(later);
        if (earlierDate != null && laterDate != null && laterDate.isBefore(earlierDate)) {
            errors.add(issue(prefix + "." + laterName, ValidationCode.OUT_OF_RANGE,
                    ValidationSeverity.ERROR,
                    laterName + " must be on or after " + earlierName, page(later)));
        }
    }

    private void validateClosingWithinProposed(String prefix, JsonNode facility,
                                               List<ValidationIssue> errors) {
        JsonNode closingField = facility.get("closingCommitment");
        BigDecimal closing = parseAmount(closingField);
        BigDecimal proposed = parseAmount(facility.get("proposedCommitmentAmount"));
        if (closing != null && proposed != null && closing.compareTo(proposed) > 0) {
            errors.add(issue(prefix + ".closingCommitment", ValidationCode.BUSINESS_RULE,
                    ValidationSeverity.ERROR,
                    "Closing Commitment must not exceed Proposed Commitment", page(closingField)));
        }
    }

    private void validateTermLoanExpiry(String prefix, JsonNode facility,
                                        List<ValidationIssue> errors) {
        JsonNode typeField = facility.get("facilityType");
        String type = text(typeField);
        if (type != null && type.toLowerCase().contains("term loan")
                && text(facility.get("expiryDate")) == null) {
            errors.add(issue(prefix + ".expiryDate", ValidationCode.BUSINESS_RULE,
                    ValidationSeverity.ERROR, "Expiry Date is required for a Term Loan facility",
                    page(typeField)));
        }
    }

    private void validatePricingRateBasis(String prefix, JsonNode facility,
                                          List<ValidationIssue> errors) {
        JsonNode pricingList = array(facility, "facilityInterestPricingList");
        if (pricingList == null) {
            return;
        }
        for (int i = 0; i < pricingList.size(); i++) {
            JsonNode pricing = pricingList.get(i);
            JsonNode baseRate = pricing.get("baseRate");
            if (text(baseRate) != null && text(pricing.get("rateBasis")) == null) {
                errors.add(issue(prefix + ".facilityInterestPricingList[" + i + "].rateBasis",
                        ValidationCode.BUSINESS_RULE, ValidationSeverity.ERROR,
                        "Rate Basis is required when an indexed Base Rate is present",
                        page(baseRate)));
            }
        }
    }

    private void validateCrossCurrency(JsonNode deal, List<ValidationIssue> warnings) {
        String dealCurrency = text(deal.get("currency"));
        if (dealCurrency != null && !"USD".equalsIgnoreCase(dealCurrency)) {
            warnings.add(issue("currency", ValidationCode.BUSINESS_RULE, ValidationSeverity.WARNING,
                    "Deal currency is " + dealCurrency
                            + "; confirm each facility currency matches the deal currency", null));
        }
    }

    private JsonNode array(JsonNode object, String property) {
        if (object == null || object.isNull()) {
            return null;
        }
        JsonNode value = object.get(property);
        if (value == null || value.isNull()) {
            return null;
        }
        if (!value.isArray()) {
            throw new IllegalArgumentException(property + " must be a JSON array");
        }
        return value;
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

    private String text(JsonNode field) {
        if (field == null || field.isNull()) {
            return null;
        }
        JsonNode value = field.isObject() ? field.get("value") : field;
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText().trim();
        return text.isEmpty() ? null : text;
    }

    private Integer page(JsonNode field) {
        if (field == null || !field.isObject()) {
            return null;
        }
        JsonNode page = field.get("pageNumber");
        return page != null && page.canConvertToInt() ? page.intValue() : null;
    }

    private LocalDate parseIso(JsonNode field) {
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

    private BigDecimal parseAmount(JsonNode field) {
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
