package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.OpenOmissionCaseCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.GracePeriod;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Opens scheduled-intake omission tracking without changing the source intake or sending messages.
 */
@Service
public class OpenOmissionCaseCommandHandler {

  private final IOmissionCaseRepository repository;
  private final Duration gracePeriodLength;

  /**
   * Injects case persistence and a nonnegative clinical grace interval; zero expires immediately.
   */
  public OpenOmissionCaseCommandHandler(
      IOmissionCaseRepository repository,
      @Value("${tata.omission.grace-period:PT30M}") Duration gracePeriodLength) {
    if (gracePeriodLength == null || gracePeriodLength.isNegative())
      throw new IllegalArgumentException("grace period must be nonnegative");
    this.repository = repository;
    this.gracePeriodLength = gracePeriodLength;
  }

  /**
   * Opens one case per intake. The scheduled time anchors grace even when processing is delayed.
   */
  @Transactional
  public OmissionCase handle(OpenOmissionCaseCommand command) {
    return repository
        .findByIntakeId(command.intakeId())
        .orElseGet(
            () ->
                repository.save(
                    new OmissionCase(
                        command.intakeId(),
                        command.olderAdultId(),
                        command.medicationName(),
                        command.scheduledAt(),
                        GracePeriod.startingAt(command.scheduledAt(), gracePeriodLength))));
  }
}
