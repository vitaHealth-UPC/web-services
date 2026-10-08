package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.EscalateOmissionCommand;
import com.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.tata.omissionescalation.domain.model.commands.RegisterOmissionCommand;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class EvaluateGracePeriodCommandHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(EvaluateGracePeriodCommandHandler.class);

  private final IOmissionCaseRepository repository;
  private final RegisterOmissionCommandHandler registerOmissionHandler;
  private final GenerateCaregiverAlertCommandHandler generateAlertHandler;
  private final EscalateOmissionCommandHandler escalateHandler;

  public EvaluateGracePeriodCommandHandler(
      IOmissionCaseRepository repository,
      RegisterOmissionCommandHandler registerOmissionHandler,
      GenerateCaregiverAlertCommandHandler generateAlertHandler,
      EscalateOmissionCommandHandler escalateHandler) {
    this.repository = repository;
    this.registerOmissionHandler = registerOmissionHandler;
    this.generateAlertHandler = generateAlertHandler;
    this.escalateHandler = escalateHandler;
  }

  /**
   * Safe to run repeatedly. Expired pending cases become omissions with their alert, and omitted
   * cases are escalated when due. Each case is processed in its own transaction so one failure does
   * not block the others.
   */
  public void handle(EvaluateGracePeriodCommand command) {
    for (OmissionCase expired : repository.findExpiredPending(command.now())) {
      runSafely(expired.getId(), () -> {
        registerOmissionHandler.handle(new RegisterOmissionCommand(expired.getId(), command.now()));
        generateAlertHandler.handle(
            new GenerateCaregiverAlertCommand(expired.getId(), command.now()));
      });
    }
    for (OmissionCase omitted : repository.findOmittedOrEscalated()) {
      runSafely(omitted.getId(), () -> escalateHandler.handle(
          new EscalateOmissionCommand(omitted.getId(), command.now())));
    }
  }

  private void runSafely(Long omissionCaseId, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException exception) {
      LOGGER.error("Omission case {} could not be evaluated", omissionCaseId, exception);
    }
  }
}
