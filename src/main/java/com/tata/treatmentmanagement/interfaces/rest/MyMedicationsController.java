package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.identitysubscription.application.models.AuthenticatedSession;
import com.tata.treatmentmanagement.interfaces.rest.resources.MedicationCatalogResource;
import com.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResourceAssembler;
import com.tata.treatmentmanagement.application.queryservices.MedicationCatalogQueryService;
import com.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@RestController
@Tag(name = "Medications")
public class MyMedicationsController {
    private final MedicationCatalogQueryService queries;

    public MyMedicationsController(MedicationCatalogQueryService queries) {
        this.queries = queries;
    }

    @GetMapping("/api/v1/me/medications")
    @Operation(summary = "Consult my medication catalog", description = "The older adult is derived from the authenticated PIN session; no owner selector is accepted.")
    public List<MedicationCatalogResource> list(@AuthenticationPrincipal AuthenticatedSession session) {
        if (session == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        if (session.role() != AuthenticatedSession.Role.OLDER_ADULT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return queries.handle(new GetMyMedicationCatalogQuery(session.subjectId())).stream()
                .map(TreatmentResourceAssembler::toResource).toList();
    }
}
