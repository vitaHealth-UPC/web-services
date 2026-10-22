package com.vitahealth.tata.identitysubscription.application.internal.queryservices;

import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import com.vitahealth.tata.identitysubscription.application.exceptions.ResourceAccessDenied;
import com.vitahealth.tata.identitysubscription.application.queryservices.ResourceAuthorizationQueryService;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import com.vitahealth.tata.treatmentmanagement.interfaces.acl.TreatmentContextFacade;
import java.util.Set;
import org.springframework.stereotype.Service;

/** Coordinates context facades to authorize clinical resource ownership and setup restrictions. */
@Service
public class ResourceAuthorizationQueryServiceImpl implements ResourceAuthorizationQueryService {
  private final CareLinkContextFacade links;
  private final IntakeContextFacade intakes;
  private final TreatmentContextFacade treatments;

  /** Injects public context contracts used to resolve relationship and resource ownership. */
  public ResourceAuthorizationQueryServiceImpl(
      CareLinkContextFacade links, IntakeContextFacade intakes, TreatmentContextFacade treatments) {
    this.links = links;
    this.intakes = intakes;
    this.treatments = treatments;
  }

  /** Restricts relationship setup credentials to their own consent and session lifecycle routes. */
  @Override
  public void authorizeRequest(AuthenticatedSession s, String path, String method) {
    if (s.role() != Role.LINK_SETUP) return;
    if (path.equals("/api/v1/sessions/current")
        || path.equals("/api/v1/sessions") && method.equals("DELETE")) return;
    if (method.equals("POST") && path.equals("/api/v1/care-links/" + s.careLinkId() + "/consent"))
      return;
    if (method.equals("GET")
        && (path.equals("/api/v1/care-links/" + s.careLinkId())
            || path.equals("/api/v1/older-adults/" + s.subjectId()))) return;
    deny();
  }

  /** Checks actor identity or resolves a resource's adult owner through its context facade. */
  @Override
  public void authorizeSelector(
      AuthenticatedSession s, String selector, String value, String path, String method) {
    if (!Set.of(
            "caregiverId",
            "familiarId",
            "accountId",
            "userId",
            "olderAdultId",
            "careLinkId",
            "medicationId",
            "treatmentId",
            "intakeId")
        .contains(selector)) return;
    if (value == null || value.isBlank()) deny();
    switch (selector) {
      case "caregiverId", "familiarId", "accountId" -> {
        if (s.role() != Role.CAREGIVER || !s.subjectId().equals(value)) deny();
      }
      case "userId" -> {
        if (s.role() == Role.LINK_SETUP || !s.subjectId().equals(value)) deny();
      }
      case "olderAdultId" -> {
        if (path.equals("/api/v1/care-links/authorization") && s.role() == Role.CAREGIVER) return;
        boolean onboarding =
            s.role() == Role.CAREGIVER
                && (path.equals("/api/v1/care-links/linking-codes")
                    || method.equals("GET") && path.equals("/api/v1/older-adults/" + value)
                    || method.equals("PUT")
                        && (path.equals("/api/v1/older-adults/" + value)
                            || path.equals(
                                "/api/v1/older-adults/" + value + "/emergency-contact")));
        authorizeAdult(s, value, onboarding);
      }
      case "careLinkId" -> {
        var link = links.getById(value);
        if (s.role() == Role.CAREGIVER) {
          if (!s.subjectId().equals(link.caregiverId()) || path.endsWith("/consent")) deny();
        } else if (!s.subjectId().equals(link.olderAdultId()) || !value.equals(s.careLinkId()))
          deny();
      }
      case "medicationId" ->
          authorizeAdult(
              s,
              treatments
                  .findMedication(value)
                  .orElseThrow(ResourceAccessDenied::new)
                  .olderAdultId(),
              false);
      case "treatmentId" ->
          authorizeAdult(
              s,
              treatments.findTreatment(value).orElseThrow(ResourceAccessDenied::new).olderAdultId(),
              false);
      case "intakeId" ->
          authorizeAdult(
              s,
              intakes.findIntake(value).orElseThrow(ResourceAccessDenied::new).olderAdultId(),
              false);
      default -> {}
    }
  }

  private void authorizeAdult(AuthenticatedSession s, String owner, boolean onboarding) {
    if (s.role() != Role.CAREGIVER) {
      if (!s.subjectId().equals(owner)) deny();
      return;
    }
    if (links.isAuthorized(s.subjectId(), owner)) return;
    if (onboarding
        && links
            .findOlderAdult(owner)
            .filter(p -> s.subjectId().equals(p.registeredByCaregiverId()))
            .isPresent()) return;
    deny();
  }

  private static void deny() {
    throw new ResourceAccessDenied();
  }
}
