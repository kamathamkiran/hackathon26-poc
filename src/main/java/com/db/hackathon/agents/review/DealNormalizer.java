package com.db.hackathon.agents.review;

import com.db.hackathon.model.extraction.Deal;
import com.db.hackathon.model.extraction.DealAdminAgent;
import com.db.hackathon.model.extraction.DealAdminServicingGroup;
import com.db.hackathon.model.extraction.Facility;
import com.db.hackathon.model.extraction.FacilityInterestPricing;
import com.db.hackathon.model.extraction.InterestPricingOption;
import com.db.hackathon.model.extraction.LoanPurpose;
import com.db.hackathon.model.extraction.Risk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Slf4j
@Component
public class DealNormalizer {

    public Deal normalize(Deal deal) {
        if (deal == null) {
            log.debug("Normalize skipped: deal is null");
            return null;
        }

        if (deal.getDealAdminAgent() == null) {
            deal.setDealAdminAgent(new DealAdminAgent());
        }
        if (deal.getDealAdminAgent().getDealAdminServicingGroup() == null) {
            deal.getDealAdminAgent().setDealAdminServicingGroup(new DealAdminServicingGroup());
        }


        deal.setInterestPricingOptions(
                seedIfEmpty(deal.getInterestPricingOptions(), InterestPricingOption::new));

        deal.setFacilityList(
                seedIfEmpty(deal.getFacilityList(), Facility::new));

        for (Facility facility : deal.getFacilityList()) {
            if (facility.getRisk() == null) {
                facility.setRisk(new Risk());
            }
            if (facility.getLoanPurpose() == null) {
                facility.setLoanPurpose(new LoanPurpose());
            }
            facility.setFacilityInterestPricingList(
                    seedIfEmpty(facility.getFacilityInterestPricingList(), FacilityInterestPricing::new));
        }

        log.debug("Deal normalized: nested objects instantiated and empty lists seeded with a template");
        return deal;
    }

    private <T> List<T> seedIfEmpty(List<T> list, Supplier<T> factory) {
        if (list == null) {
            list = new ArrayList<>();
        }
        if (list.isEmpty()) {
            list.add(factory.get());
        }
        return list;
    }
}
