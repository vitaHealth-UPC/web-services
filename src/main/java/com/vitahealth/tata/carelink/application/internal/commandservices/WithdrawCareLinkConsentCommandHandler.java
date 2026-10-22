package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.result.ConsentWithdrawalResult;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConsentWithdrawn;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Withdraws adult consent and publishes credential invalidation and history evidence atomically.
 */
@Service
@Transactional
public class WithdrawCareLinkConsentCommandHandler {
  private final CareLinkRepository links;
  private final Clock clock;
  private final ApplicationEventPublisher events;

  /** Injects relationship reservations, observable time and in-transaction event publication. */
  public WithdrawCareLinkConsentCommandHandler(
      CareLinkRepository links, Clock clock, ApplicationEventPublisher events) {
    this.links = links;
    this.clock = clock;
    this.events = events;
  }

  /** Reserves the relationship, validates the adult actor and preserves the prior approval. */
  public ConsentWithdrawalResult handle(WithdrawCareLinkConsentCommand command) {
    var link =
        links
            .findByIdForUpdate(command.careLinkId())
            .orElseThrow(
                () ->
                    new CareLinkApplicationException(
                        CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND,
                        "care link not found"));
    if (!link.olderAdultId().equals(command.actingOlderAdultId())) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CONSENT_REQUIRED,
          "only the linked adult can withdraw consent");
    }
    var approvedAt = link.consent().recordedAt();
    var now = clock.instant();
    final boolean changed;
    try {
      changed = link.withdrawConsent(command.actingOlderAdultId(), now);
    } catch (IllegalStateException exception) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CONSENT_REQUIRED, exception.getMessage());
    }
    if (!changed) return new ConsentWithdrawalResult(link.id(), false);
    var saved = links.save(link);
    events.publishEvent(
        new CareLinkConsentWithdrawn(
            saved.id(), saved.caregiverId(), saved.olderAdultId(), approvedAt, now));
    return new ConsentWithdrawalResult(saved.id(), true);
  }
}
