package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.domain.model.commands.ResolveOmissionCaseCommand;
import com.tata.omissionescalation.application.internal.outboundservices.IntakeOmissionPort;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveOmissionCaseCommandHandler {

  private final IOmissionCaseRepository repository;
  private final IntakeOmissionPort intakes;

  public ResolveOmissionCaseCommandHandler(IOmissionCaseRepository repository, IntakeOmissionPort intakes) {
    this.repository = repository;
    this.intakes = intakes;
  }

  /** Applies the recorded confirmation once; an omitted or resolved case cannot be reopened. */
  @Transactional
  public void handle(ResolveOmissionCaseCommand command) {
    if (!intakes.lockRecordedConfirmation(command.intakeId(), command.confirmedAt())) return;
    repository.findByIntakeId(command.intakeId())
        .flatMap(c -> repository.findByIdForUpdate(c.getId()))
        .filter(c -> c.getStatus() == OmissionCaseStatus.PENDING)
        .ifPresent(c -> {
          c.resolve();
          repository.save(c);
        });
  }
}
