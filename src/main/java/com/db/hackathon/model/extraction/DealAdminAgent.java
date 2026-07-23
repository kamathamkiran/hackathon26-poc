package com.db.hackathon.model.extraction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DealAdminAgent {

    private ExtractedField customerExternalId;

    private DealAdminServicingGroup dealAdminServicingGroup;
}
