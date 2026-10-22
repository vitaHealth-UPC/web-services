package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.domain.model.commands.EscalateOmissionCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EvaluateGracePeriodCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.GenerateCaregiverAlertCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.RegisterOmissionCommand;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Coordinates omission registration, missing-alert recovery, and due escalation per active case.
 */
@Service
public class EvaluateGracePeriodCommandHandler {

  private static final Logger LOGGER =
      LoggerFactory.getLogger(EvaluateGracePeriodCommandHandler.class);

  private final IOmissionCaseRepository repository;
  private final RegisterOmissionCommandHandler registerOmissionHandler;
  private final GenerateCaregiverAlertCommandHandler generateAlertHandler;
  private final EscalateOmissionCommandHandler escalateHandler;

  /** Injects work selection and transaction-owning case commands. */
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
   * Recovers missing business alerts before registering newly expired cases and escalating due
   * cases. Suspends any caller transaction so each command commits or rolls back independently;
   * failed case work cannot poison the caller or subsequent cases.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public void handle(EvaluateGracePeriodCommand command) {
    recoverMissingAlerts(command);
    processBatches(
        cursor -> repository.findExpiredPendingIdsAfter(command.now(), cursor, 100),
        id -> {
          registerOmissionHandler.handle(new RegisterOmissionCommand(id, command.now()));
          generateAlertHandler.handle(new GenerateCaregiverAlertCommand(id, command.now()));
        });
    processBatches(
        cursor -> repository.findActiveCaseIdsAfter(cursor, 100),
        id -> escalateHandler.handle(new EscalateOmissionCommand(id, command.now())));
  }

  private void recoverMissingAlerts(EvaluateGracePeriodCommand command) {
    processBatches(
        cursor -> repository.findActiveCaseIdsWithoutAlertAfter(cursor, 100),
        id -> generateAlertHandler.handle(new GenerateCaregiverAlertCommand(id, command.now())));
  }

  private void processBatches(LongFunction<List<Long>> selection, Consumer<Long> action) {
    long cursor = 0;
    while (true) {
      var ids = selection.apply(cursor);
      if (ids.isEmpty()) return;
      for (var id : ids) {
        if (id == null || id <= cursor)
          throw new IllegalStateException("omission evaluation cursor did not advance");
        runSafely(id, () -> action.accept(id));
        cursor = id;
      }
    }
  }

  private void runSafely(Long omissionCaseId, Runnable action) {
    try {
      action.run();
    } catch (RuntimeException exception) {
      LOGGER.error(
          "Omission case {} could not be evaluated; category=CASE_EVALUATION_FAILED",
          omissionCaseId);
    }
  }
}
