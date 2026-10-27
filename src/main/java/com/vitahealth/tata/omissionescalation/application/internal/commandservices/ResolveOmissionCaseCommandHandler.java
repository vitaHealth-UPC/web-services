package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import com.vitahealth.tata.omissionescalation.domain.model.commands.ResolveOmissionCaseCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Resolves pending tracking only when Intake Execution confirms the exact recorded occurrence. */
@Service
public class ResolveOmissionCaseCommandHandler {

  private final IOmissionCaseRepository repository;
  private final IntakeOmissionPort intakes;
  private final Clock clock;

  /** Injects case storage and the source confirmation reservation boundary. */
  public ResolveOmissionCaseCommandHandler(
      IOmissionCaseRepository repository, IntakeOmissionPort intakes, Clock clock) {
    this.repository = repository;
    this.intakes = intakes;
    this.clock = clock;
  }

  /** Resolves pending tracking or closes a historical omission after an offline confirmation. */
  @Transactional
  public void handle(ResolveOmissionCaseCommand command) {
    if (!intakes.lockRecordedConfirmation(command.intakeId(), command.confirmedAt())) return;
    repository
        .findByIntakeId(command.intakeId())
        .flatMap(c -> repository.findByIdForUpdate(c.getId()))
        .ifPresent(
            c -> {
              if (c.getStatus() == OmissionCaseStatus.PENDING) {
                c.resolve();
              } else if (c.getStatus() == OmissionCaseStatus.OMITTED
                  || c.getStatus() == OmissionCaseStatus.ESCALATED) {
                c.close(clock.instant());
              } else {
                return;
              }
              repository.save(c);
            });
  }
}
