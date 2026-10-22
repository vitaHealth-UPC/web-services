package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.commands.ConfirmCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.events.CareLinkConfirmed;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.services.CareLinkConfirmationPolicy;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Owns confirmation policy, persistence and its event within the consent transaction. */
@Service
@Transactional(propagation = Propagation.MANDATORY)
public class ConfirmCareLinkCommandHandler {
  private final CareLinkRepository links;
  private final ApplicationEventPublisher events;
  private final CareLinkConfirmationPolicy policy = new CareLinkConfirmationPolicy();

  /** Injects relationship persistence and transactional event publication. */
  public ConfirmCareLinkCommandHandler(CareLinkRepository links, ApplicationEventPublisher events) {
    this.links = links;
    this.events = events;
  }

  /**
   * Confirms approved evidence once without opening a separate transaction or replacing history.
   */
  public CareLinkResult handle(ConfirmCareLinkCommand command) {
    var link =
        links
            .findByIdForUpdate(command.careLinkId())
            .orElseThrow(
                () ->
                    new CareLinkApplicationException(
                        CareLinkApplicationException.Code.CARE_LINK_NOT_FOUND,
                        "care link not found"));
    if (link.isActive()) return CareLinkMapper.toResult(link);
    if (!policy.canConfirm(link, command.confirmedAt())) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.CONSENT_REQUIRED, "care link cannot be confirmed");
    }
    link.confirm(command.confirmedAt());
    var saved = links.save(link);
    events.publishEvent(
        new CareLinkConfirmed(saved.id(), saved.caregiverId(), saved.olderAdultId()));
    return CareLinkMapper.toResult(saved);
  }
}
