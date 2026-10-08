package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.domain.factories.OmissionCaseFactory;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.OpenOmissionCaseCommand;
import com.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Duration;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpenOmissionCaseCommandHandler {

  private final IOmissionCaseRepository repository;
  private final Duration gracePeriodLength;

  public OpenOmissionCaseCommandHandler(
      IOmissionCaseRepository repository,
      @Value("${tata.omission.grace-period:PT30M}") Duration gracePeriodLength) {
    this.repository = repository;
    this.gracePeriodLength = gracePeriodLength;
  }

  /** Opens the case for the intake; if it already exists it is returned unchanged. */
  @Transactional
  public OmissionCase handle(OpenOmissionCaseCommand command) {
    return repository.findByIntakeId(command.intakeId())
        .orElseGet(() -> repository.save(OmissionCaseFactory.createPending(
            command.intakeId(),
            command.olderAdultId(),
            command.medicationName(),
            command.scheduledAt(),
            GracePeriod.startingAt(Instant.now(), gracePeriodLength))));
  }
}
