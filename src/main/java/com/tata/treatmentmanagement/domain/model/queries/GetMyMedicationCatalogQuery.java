package com.tata.treatmentmanagement.domain.model.queries;

import static com.tata.shared.domain.validation.DomainText.requireText;

public record GetMyMedicationCatalogQuery(String olderAdultId) {
    public GetMyMedicationCatalogQuery {
        olderAdultId = requireText(olderAdultId, "olderAdultId");
    }
}
