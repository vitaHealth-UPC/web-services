package com.vitahealth.tata.carelink.interfaces.rest;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.commandservices.LinkAuthenticationCommandService;
import com.vitahealth.tata.carelink.application.queryservices.CareLinkQueryService;
import com.vitahealth.tata.carelink.domain.model.commands.EndCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.IssueAdultAccessCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RedeemAdultAccessCodeCommand;
import com.vitahealth.tata.carelink.domain.model.queries.CheckCareRelationshipAuthorizationQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetCareLinkByIdQuery;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinkSummariesQuery;
import com.vitahealth.tata.carelink.interfaces.rest.resources.AcceptCareLinkResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.AdultAccessCodeResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.AdultAccessRedemptionResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.CaregiverDecisionResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.CaregiverRequestResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.GenerateLinkingCodeResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.IssueAdultAccessCodeResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.JoinAsCaregiverResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.RedeemAdultAccessCodeResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.RegisterConsentResource;
import com.vitahealth.tata.carelink.interfaces.rest.transform.AcceptCareLinkCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.AuthenticatedCareLinkResourceFromResultAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkResourceFromResultAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.CareLinkResponseAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.ConfirmedCareLinkResourceFromResultAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.GenerateLinkingCodeCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.RegisterConsentCommandFromResourceAssembler;
import com.vitahealth.tata.carelink.interfaces.rest.transform.WithdrawCareLinkConsentCommandFromIdentityAssembler;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
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

  /** Ends the caregiver's own relationship and revokes the adult access scoped to it. */
  @DeleteMapping("/{careLinkId}")
  @Operation(
      summary = "End a care link",
      description =
          "The caregiver stops caring for the adult (removes the person from the family list)."
              + " Other caregivers keep their relationships; history is preserved.")
  public ResponseEntity<?> end(
      @PathVariable String careLinkId, @RequestParam String caregiverId) {
    return responses.toNoContent(
        commandService.end(new EndCareLinkCommand(careLinkId, caregiverId)));
  }

  /** Issues a new sign-in code for the adult of a confirmed relationship. */
  @PostMapping("/{careLinkId}/access-codes")
  @Operation(
      summary = "Issue an adult sign-in code",
      description =
          "Single-use code valid for 24 hours that lets the adult sign in on a new or reinstalled"
              + " device. Issuing a new one invalidates the previous unused code.")
  public ResponseEntity<?> issueAccessCode(
      @PathVariable String careLinkId,
      @Valid @RequestBody IssueAdultAccessCodeResource resource) {
    return responses.toResponse(
        commandService.issueAccessCode(
            new IssueAdultAccessCodeCommand(careLinkId, resource.caregiverId())),
        HttpStatus.CREATED,
        result ->
            new AdultAccessCodeResource(result.careLinkId(), result.code(), result.expiresAt()));
  }

  /** Redeems a sign-in code on the adult's device; public, the code is the credential. */
  @PostMapping("/access-code-redemptions")
  @Operation(
      summary = "Redeem an adult sign-in code",
      description =
          "Identifies the adult on this device. If the adult has a PIN no session is returned and"
              + " the device must sign in with it; otherwise a session allows creating the PIN.")
  public ResponseEntity<?> redeemAccessCode(
      @Valid @RequestBody RedeemAdultAccessCodeResource resource) {
    return responses.toResponse(
        commandService.redeemAccessCode(new RedeemAdultAccessCodeCommand(resource.code())),
        result ->
            new AdultAccessRedemptionResource(
                result.careLinkId(),
                result.olderAdultId(),
                result.olderAdultName(),
                result.pinRegistered(),
                result.accessToken(),
                result.expiresAt()));
  }

  /** A caregiver of the adult invites another caregiver with a code. */
  @PostMapping("/{careLinkId}/caregiver-invitations")
  @Operation(
      summary = "Invite another caregiver",
      description =
          "Single-use code valid for 24 hours. The invited caregiver joins with it and waits until"
              + " the adult accepts.")
  public ResponseEntity<?> inviteCaregiver(
      @PathVariable String careLinkId,
      @Valid @RequestBody IssueAdultAccessCodeResource resource) {
    return responses.toResponse(
        commandService.inviteCaregiver(careLinkId, resource.caregiverId()),
        HttpStatus.CREATED,
        result ->
            new AdultAccessCodeResource(result.careLinkId(), result.code(), result.expiresAt()));
  }

  /** Another caregiver joins with an invitation; the adult still has to accept. */
  @PostMapping("/caregiver-joins")
  @Operation(
      summary = "Join as another caregiver",
      description = "Creates a relationship that waits for the adult's decision.")
  public ResponseEntity<?> joinAsCaregiver(@Valid @RequestBody JoinAsCaregiverResource resource) {
    return responses.toResponse(
        commandService.joinAsCaregiver(resource.caregiverId(), resource.code()),
        HttpStatus.CREATED,
        CareLinkResourceFromResultAssembler::toResource);
  }

  /** Caregivers waiting for the signed-in adult's decision. */
  @GetMapping("/caregiver-requests")
  @Operation(summary = "List caregiver requests for the signed-in adult")
  public ResponseEntity<?> caregiverRequests(
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return responses.toResponse(
        commandService.caregiverRequests(adult(session)),
        requests ->
            requests.stream()
                .map(
                    request ->
                        new CaregiverRequestResource(
                            request.careLinkId(),
                            request.caregiverId(),
                            request.caregiverName(),
                            request.requestedAt()))
                .toList());
  }

  /** The signed-in adult accepts or rejects another caregiver. */
  @PostMapping("/caregiver-requests/{requestId}/decision")
  @Operation(summary = "Accept or reject another caregiver")
  public ResponseEntity<?> decideCaregiverRequest(
      @PathVariable String requestId,
      @Valid @RequestBody CaregiverDecisionResource resource,
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return responses.toResponse(
        commandService.decideCaregiverRequest(adult(session), requestId, resource.accepted()),
        CareLinkResourceFromResultAssembler::toResource);
  }

  /** The signed-in adult removes their account: every relationship ends. */
  @DeleteMapping("/adult-account")
  @Operation(
      summary = "Remove the signed-in adult's account",
      description =
          "Ends every care relationship of the adult and deletes their PIN. Caregivers stop"
              + " seeing the adult; history is kept for audit only.")
  public ResponseEntity<?> removeAdultAccount(
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return responses.toNoContent(commandService.removeAdultAccount(adult(session)));
  }

  private static String adult(AuthenticatedSession session) {
    if (session == null || session.role() != Role.OLDER_ADULT)
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CONSENT_REQUIRED, "only the adult can do this");
    return session.subjectId();
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
