package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.ConsentDecisionResult;
import com.vitahealth.tata.carelink.domain.model.commands.ConfirmCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records an explicit decision and coordinates confirmation in the same write transaction. */
@Service
@Transactional
public class RegisterConsentCommandHandler {
  private final CareLinkRepository links;
  private final Clock clock;
  private final ConfirmCareLinkCommandHandler confirmation;

  /**
   * Injects decision persistence, observable time and the dedicated confirmation responsibility.
   */
  public RegisterConsentCommandHandler(
      CareLinkRepository links, Clock clock, ConfirmCareLinkCommandHandler confirmation) {
    this.links = links;
    this.clock = clock;
    this.confirmation = confirmation;
  }

  /** Returns the relationship projection for callers that do not issue credentials. */
  public CareLinkResult handle(RegisterConsentCommand command) {
    return handleDecision(command).link();
  }

  /** Distinguishes first decisions from matching replays under the same relationship lock. */
  public ConsentDecisionResult handleDecision(RegisterConsentCommand command) {
    var link =
        links
            .findByIdForUpdate(command.careLinkId())
            .orElseThrow(
                () ->
                    new CareLinkApplicationException(
                        CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND,
                        "care link not found"));
    if (command.accepted() && link.isActive()
        || !command.accepted()
            && link.status() == CareLinkStatus.REVOKED
            && link.confirmedAt() == null
            && link.consent().recordedAt() != null) {
      return new ConsentDecisionResult(CareLinkMapper.toResult(link), false);
    }
    var now = clock.instant();
    try {
      link.registerConsent(command.accepted(), now);
    } catch (IllegalStateException exception) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CONSENT_REQUIRED, exception.getMessage());
    }
    var saved = links.save(link);
    if (!command.accepted()) return new ConsentDecisionResult(CareLinkMapper.toResult(saved), true);
    return new ConsentDecisionResult(
        confirmation.handle(new ConfirmCareLinkCommand(saved.id(), now)), true);
  }
}
