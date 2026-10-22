package com.vitahealth.tata.intakeexecution.application.internal.acl;

import com.vitahealth.tata.intakeexecution.application.internal.commandservices.IntakeEvidenceVerificationService;
import com.vitahealth.tata.intakeexecution.application.internal.queryservices.GetIntakeHistoryQueryHandler;
import com.vitahealth.tata.intakeexecution.application.internal.queryservices.GetNextIntakeQueryHandler;
import com.vitahealth.tata.intakeexecution.application.internal.queryservices.IntakeEvidenceQueryService;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.interfaces.acl.IntakeContextFacade;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;

/** Adapts the public cross-context contract to intake queries and evidence verification. */
@Service
public class IntakeContextFacadeImpl implements IntakeContextFacade {
  private final IntakeEvidenceQueryService evidence;
  private final IntakeEvidenceVerificationService verification;
  private final GetIntakeHistoryQueryHandler history;
  private final GetNextIntakeQueryHandler next;

  /** Injects transactional capabilities without owning persistence or clinical decisions. */
  public IntakeContextFacadeImpl(
      IntakeEvidenceQueryService evidence,
      IntakeEvidenceVerificationService verification,
      GetIntakeHistoryQueryHandler history,
      GetNextIntakeQueryHandler next) {
    this.evidence = evidence;
    this.verification = verification;
    this.history = history;
    this.next = next;
  }

  @Override
  public List<IntakeResult> findEvidence(String owner, Instant from, Instant to) {
    return evidence.findEvidence(owner, from, to);
  }

  @Override
  public List<String> findOlderAdultIdsWithIntakes(
      Instant from, Instant to, String after, int limit) {
    return evidence.findOlderAdultIdsWithIntakes(from, to, after, limit);
  }

  /** Reads definitive history within the validated integration range. */
  @Override
  public List<IntakeResult> history(String owner, Instant from, Instant to) {
    return history.handle(owner, from, to);
  }

  /** Reads the earliest pending evidence without creating a new intake. */
  @Override
  public Optional<IntakeResult> next(String owner) {
    return next.handle(owner);
  }

  /** Resolves existing evidence for trusted cross-context ownership checks. */
  @Override
  public Optional<IntakeResult> findIntake(String id) {
    return evidence.findIntake(id);
  }

  /** Reserves current pending evidence in the caller transaction. */
  @Override
  public boolean lockPendingIntake(String id) {
    return verification.lockPendingIntake(id);
  }

  @Override
  public boolean lockRecordedConfirmation(String id, Instant confirmedAt) {
    return verification.lockRecordedConfirmation(id, confirmedAt);
  }
}
