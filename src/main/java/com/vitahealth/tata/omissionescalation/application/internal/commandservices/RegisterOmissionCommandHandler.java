package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.RegisterOmissionCommand;
import com.vitahealth.tata.omissionescalation.domain.model.events.IntakeOmitted;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers definitive omission while preserving the shared intake-before-case lock order. */
@Service
public class RegisterOmissionCommandHandler {

  static final String OMISSION_REASON = "Intake not confirmed within the grace period";

  private final IOmissionCaseRepository repository;
  private final IDomainEventPublisher eventPublisher;
  private final IntakeOmissionPort intakes;

  /** Injects current intake authorization, case storage and atomic local omission publication. */
  public RegisterOmissionCommandHandler(
      IOmissionCaseRepository repository,
      IDomainEventPublisher eventPublisher,
      IntakeOmissionPort intakes) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.intakes = intakes;
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
    // Confirmation also locks the intake first; use the same order to avoid a lock cycle.
    if (!intakes.lockPendingIntake(omissionCase.getIntakeId())) return;
    omissionCase = repository.findByIdForUpdate(command.omissionCaseId()).orElse(null);
    if (omissionCase == null
        || omissionCase.getStatus() != OmissionCaseStatus.PENDING
        || !omissionCase.isGraceExpired(command.now())) return;
    omissionCase.markOmitted(command.now());
    repository.save(omissionCase);
    eventPublisher.publish(
        new IntakeOmitted(
            omissionCase.getIntakeId(),
            omissionCase.getOlderAdultId(),
            omissionCase.getMedicationName(),
            omissionCase.getScheduledAt(),
            OMISSION_REASON,
            command.now()));
  }
}
