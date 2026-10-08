package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.RegisterOmissionCommand;
import com.tata.omissionescalation.domain.model.events.IntakeOmitted;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegisterOmissionCommandHandler {

  static final String OMISSION_REASON = "Intake not confirmed within the grace period";

  private final IOmissionCaseRepository repository;
  private final IDomainEventPublisher eventPublisher;

  public RegisterOmissionCommandHandler(
      IOmissionCaseRepository repository, IDomainEventPublisher eventPublisher) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
  }

  /** Registers the omission once; a case that is not pending or not expired is left untouched. */
  @Transactional
  public void handle(RegisterOmissionCommand command) {
    OmissionCase omissionCase = repository.findById(command.omissionCaseId()).orElse(null);
    if (omissionCase == null
        || omissionCase.getStatus() != OmissionCaseStatus.PENDING
        || !omissionCase.isGraceExpired(command.now())) {
      return;
    }
    omissionCase.markOmitted(command.now());
    repository.save(omissionCase);
    eventPublisher.publish(new IntakeOmitted(
        omissionCase.getIntakeId(),
        omissionCase.getOlderAdultId(),
        omissionCase.getMedicationName(),
        omissionCase.getScheduledAt(),
        OMISSION_REASON,
        command.now()));
  }
}
