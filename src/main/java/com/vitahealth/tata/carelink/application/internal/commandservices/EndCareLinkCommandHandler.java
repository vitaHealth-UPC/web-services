package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.result.ConsentWithdrawalResult;
import com.vitahealth.tata.carelink.domain.model.commands.EndCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConsentWithdrawn;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.time.Clock;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lets a caregiver remove a person from "Tus familiares". Other caregivers of the same adult keep
 * their own relationships; the withdrawal event revokes the credentials scoped to this one.
 */
@Service
@Transactional
public class EndCareLinkCommandHandler {
  private final CareLinkRepository links;
  private final Clock clock;
  private final ApplicationEventPublisher events;

  /** Injects relationship reservations, observable time and event publication. */
  public EndCareLinkCommandHandler(
      CareLinkRepository links, Clock clock, ApplicationEventPublisher events) {
    this.links = links;
    this.clock = clock;
    this.events = events;
  }

  /** Ends the caregiver's own relationship; a repeated request changes nothing. */
  public ConsentWithdrawalResult handle(EndCareLinkCommand command) {
    var link =
        links
            .findByIdForUpdate(command.careLinkId())
            .filter(found -> found.caregiverId().equals(command.caregiverId()))
            .orElseThrow(
                () ->
                    new CareLinkApplicationException(
                        CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND,
                        "care link not found"));
    var approvedAt = link.consent().recordedAt();
    var now = clock.instant();
    if (!link.endByCaregiver(command.caregiverId(), now))
      return new ConsentWithdrawalResult(link.id(), false);
    var saved = links.save(link);
    events.publishEvent(
        new CareLinkConsentWithdrawn(
            saved.id(),
            saved.caregiverId(),
            saved.olderAdultId(),
            approvedAt == null ? now : approvedAt,
            now));
    return new ConsentWithdrawalResult(saved.id(), true);
  }
}
