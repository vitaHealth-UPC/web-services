package com.vitahealth.tata.carelink.interfaces.rest;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.commandservices.LinkAuthenticationCommandService;
import com.vitahealth.tata.carelink.application.queryservices.CareLinkQueryService;
import com.vitahealth.tata.carelink.domain.model.queries.CheckCareRelationshipAuthorizationQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinkSummariesQuery;
import com.vitahealth.tata.carelink.interfaces.rest.resources.AcceptCareLinkResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.GenerateLinkingCodeResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.RegisterConsentResource;
import com.vitahealth.tata.carelink.interfaces.rest.transform.AcceptCareLinkCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.AuthenticatedCareLinkResourceFromResultAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkResourceFromResultAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkResponseAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.ConfirmedCareLinkResourceFromResultAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.GenerateLinkingCodeCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.RegisterConsentCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.WithdrawCareLinkConsentCommandFromIdentityAssembler;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Exposes relationship lifecycle and lookup contracts through authenticated REST adapters. */
@RestController
@RequestMapping("/api/v1/care-links")
public class CareLinksController {
  private final CareLinkCommandService commandService;
  private final CareLinkQueryService queryService;
  private final LinkAuthenticationCommandService authentication;
  private final CareLinkResponseAssembler responses;

  /** Injects public relationship commands, queries and atomic session-onboarding coordination. */
  public CareLinksController(
      CareLinkCommandService commandService,
      CareLinkQueryService queryService,
      LinkAuthenticationCommandService authentication,
      CareLinkResponseAssembler responses) {
    this.commandService = commandService;
    this.queryService = queryService;
    this.authentication = authentication;
    this.responses = responses;
  }

  /** Issues short-lived linking evidence for an authorized caregiver profile. */
  @PostMapping("/linking-codes")
  public ResponseEntity<?> generateCode(@Valid @RequestBody GenerateLinkingCodeResource resource) {
    return responses.toResponse(
        commandService.generateLinkingCode(
            GenerateLinkingCodeCommandFromResourceAssembler.toCommand(resource)),
        HttpStatus.CREATED,
        CareLinkResourceFromResultAssembler::toResource);
  }

  /** Accepts code evidence and returns restricted consent-onboarding access. */
  @PostMapping("/acceptances")
  public ResponseEntity<?> accept(@Valid @RequestBody AcceptCareLinkResource resource) {
    return responses.toResponse(
        authentication.accept(AcceptCareLinkCommandFromResourceAssembler.toCommand(resource)),
        AuthenticatedCareLinkResourceFromResultAssembler::toResource);
  }

  /** Records the adult decision and establishes full access only for accepted consent. */
  @PostMapping("/{careLinkId}/consent")
  public ResponseEntity<?> registerConsent(
      @PathVariable String careLinkId, @Valid @RequestBody RegisterConsentResource resource) {
    return responses.toResponse(
        authentication.registerConsent(
            RegisterConsentCommandFromResourceAssembler.toCommand(careLinkId, resource)),
        AuthenticatedCareLinkResourceFromResultAssembler::toResource);
  }

  /**
   * Withdraws consent using the verified adult identity, without accepting an actor in the body.
   */
  @DeleteMapping("/{careLinkId}/consent")
  public ResponseEntity<?> withdrawConsent(
      @PathVariable String careLinkId,
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return responses.toNoContent(
        commandService.withdrawConsent(
            WithdrawCareLinkConsentCommandFromIdentityAssembler.toCommand(careLinkId, session)));
  }

  /** Reads only relationship evidence authorized by the request's session scope. */
  @GetMapping("/{careLinkId}")
  public ResponseEntity<?> get(@PathVariable String careLinkId) {
    return responses.toResponse(
        queryService.handle(new GetCareLinkByIdQuery(careLinkId)),
        CareLinkResourceFromResultAssembler::toResource);
  }

  /** Lists active consented relationships for the authenticated caregiver. */
  @GetMapping
  @Operation(
      summary = "List confirmed care links of a caregiver",
      description =
          "Returns active links with consent, newest first. An empty list means no confirmed"
              + " links.")
  public ResponseEntity<?> list(@RequestParam String caregiverId) {
    return responses.toResponse(
        queryService.handle(new GetConfirmedCareLinkSummariesQuery(caregiverId)),
        links ->
            links.stream().map(ConfirmedCareLinkResourceFromResultAssembler::toResource).toList());
  }

  /**
   * Checks current caregiver/adult relationship authorization through the public query contract.
   */
  @GetMapping("/authorization")
  public ResponseEntity<?> isAuthorized(
      @RequestParam String caregiverId, @RequestParam String olderAdultId) {
    return responses.toResponse(
        queryService.handle(new CheckCareRelationshipAuthorizationQuery(caregiverId, olderAdultId)),
        Function.identity());
  }
}
