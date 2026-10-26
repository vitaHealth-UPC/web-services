package com.vitahealth.tata.intakeexecution.application.internal.commandservices;

import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakeCommand;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ReportUnconfirmedIntakesCommand;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeReportingCandidate;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Scans ordered candidate references without coupling their reporting transactions. */
@Service
public class ReportUnconfirmedIntakesCommandHandler {

  private static final Logger LOG =
      LoggerFactory.getLogger(ReportUnconfirmedIntakesCommandHandler.class);
  private static final int PAGE_SIZE = 100;
  private final IntakeRepository repository;
  private final ReportUnconfirmedIntakeCommandHandler reporting;

  /** Injects reference scanning and the transaction-owning single-intake handler. */
  public ReportUnconfirmedIntakesCommandHandler(
      IntakeRepository repository, ReportUnconfirmedIntakeCommandHandler reporting) {
    this.repository = repository;
    this.reporting = reporting;
  }

  /**
   * Advances past every attempted candidate, including failures, so later pages can progress.
   * Failed handoffs remain unreported and are eligible for the next evaluation.
   */
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  public int handle(ReportUnconfirmedIntakesCommand command) {
    int reported = 0;
    int failed = 0;
    IntakeReportingCandidate after = null;
    while (true) {
      var candidates =
          repository.findUnreportedPendingDueAfter(command.reportedAt(), after, PAGE_SIZE);
      if (candidates.isEmpty()) break;
      for (var candidate : candidates) {
        try {
          if (reporting.handle(
              new ReportUnconfirmedIntakeCommand(
                  candidate.id(), command.cutoff(), command.reportedAt()))) reported++;
        } catch (RuntimeException handoffFailure) {
          // The proxy has rolled back this candidate; omit identity and clinical details.
          failed++;
        }
        after = candidate;
      }
      if (candidates.size() < PAGE_SIZE) break;
    }
    if (failed > 0)
      LOG.warn("Unconfirmed intake evaluation left {} failed handoffs for retry", failed);
    return reported;
  }
}
