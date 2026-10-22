package com.vitahealth.tata.carelink.interfaces.rest;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.queryservices.CareLinkQueryService;
import com.vitahealth.tata.carelink.domain.model.queries.GetOlderAdultProfileByIdQuery;
import com.vitahealth.tata.carelink.interfaces.rest.resources.AssociateEmergencyContactResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.RegisterOlderAdultProfileResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.UpdateOlderAdultProfileResource;
import com.vitahealth.tata.carelink.interfaces.rest.transform.AssociateEmergencyContactCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkResponseAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.OlderAdultProfileResourceFromResultAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.RegisterOlderAdultProfileCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.UpdateOlderAdultProfileCommandFromResourceAssembler;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes authorized registration, reads and caregiver-owned profile changes. */
@RestController
@RequestMapping("/api/v1/older-adults")
public class OlderAdultsController {
  private final CareLinkCommandService commandService;
  private final CareLinkQueryService queryService;
  private final CareLinkResponseAssembler responses;

  /** Injects public application command and query boundaries. */
  public OlderAdultsController(
      CareLinkCommandService commandService,
      CareLinkQueryService queryService,
      CareLinkResponseAssembler responses) {
    this.commandService = commandService;
    this.queryService = queryService;
    this.responses = responses;
  }

  /** Registers profile data for the enabled caregiver identified by the request. */
  @PostMapping
  public ResponseEntity<?> register(
      @Valid @RequestBody RegisterOlderAdultProfileResource resource) {
    return responses.toResponse(
        commandService.registerOlderAdult(
            RegisterOlderAdultProfileCommandFromResourceAssembler.toCommand(resource)),
        HttpStatus.CREATED,
        OlderAdultProfileResourceFromResultAssembler::toResource);
  }

  /** Updates basic profile data while preserving registration ownership. */
  @PutMapping("/{olderAdultId}")
  public ResponseEntity<?> update(
      @PathVariable String olderAdultId,
      @Valid @RequestBody UpdateOlderAdultProfileResource resource,
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return responses.toResponse(
        commandService.updateOlderAdultProfile(
            UpdateOlderAdultProfileCommandFromResourceAssembler.toCommand(
                olderAdultId, resource, session)),
        OlderAdultProfileResourceFromResultAssembler::toResource);
  }

  /** Associates a complete emergency contact without changing care consent or push recipients. */
  @PutMapping("/{olderAdultId}/emergency-contact")
  public ResponseEntity<?> associateContact(
      @PathVariable String olderAdultId,
      @Valid @RequestBody AssociateEmergencyContactResource resource,
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return responses.toResponse(
        commandService.associateEmergencyContact(
            AssociateEmergencyContactCommandFromResourceAssembler.toCommand(
                olderAdultId, resource, session)),
        OlderAdultProfileResourceFromResultAssembler::toResource);
  }

  /** Reads the profile authorized by the current identity. */
  @GetMapping("/{olderAdultId}")
  public ResponseEntity<?> get(@PathVariable String olderAdultId) {
    return responses.toResponse(
        queryService.handle(new GetOlderAdultProfileByIdQuery(olderAdultId)),
        OlderAdultProfileResourceFromResultAssembler::toResource);
  }
}
