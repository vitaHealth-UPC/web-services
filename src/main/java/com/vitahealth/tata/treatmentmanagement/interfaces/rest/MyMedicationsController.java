package com.vitahealth.tata.treatmentmanagement.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.treatmentmanagement.application.queryservices.MedicationCatalogQueryService;
import com.vitahealth.tata.treatmentmanagement.domain.model.queries.GetMyMedicationCatalogQuery;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.MedicationCatalogResourceFromResultAssembler;
import com.vitahealth.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResponseAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exposes only the catalog of the authenticated older adult session. */
@RestController
@Tag(name = "Medications")
public class MyMedicationsController {
  private final MedicationCatalogQueryService queries;

  /** Injects the catalog application boundary. */
  public MyMedicationsController(MedicationCatalogQueryService queries) {
    this.queries = queries;
  }

  @GetMapping("/api/v1/me/medications")
  @Operation(
      summary = "Consult my medication catalog",
      description =
          "The older adult is derived from the authenticated PIN session; no owner selector is"
              + " accepted.")
  public ResponseEntity<?> list(
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    if (session == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (session.role() != AuthenticatedSession.Role.OLDER_ADULT) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    }
    return TreatmentResponseAssembler.toResponse(
        queries.handle(new GetMyMedicationCatalogQuery(session.subjectId())),
        HttpStatus.OK,
        items ->
            items.stream().map(MedicationCatalogResourceFromResultAssembler::toResource).toList());
  }
}
