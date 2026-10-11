package com.tata.intakeexecution.interfaces.acl;
import com.tata.intakeexecution.application.models.IntakeResult;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
public interface IntakeContextFacade {
 List<IntakeResult> findEvidence(String olderAdultId, Instant from, Instant to);
 List<String> findOlderAdultIdsWithIntakes(Instant from, Instant to);
 List<IntakeResult> history(String owner, Instant from, Instant to);
 Optional<IntakeResult> next(String owner);
 Optional<IntakeResult> findIntake(String id);
 /** Locks the source intake for the current transaction before deciding an omission. */
 boolean lockPendingIntake(String id);
 /** Locks and verifies the original recorded confirmation, including its timestamp. */
 boolean lockRecordedConfirmation(String id, Instant confirmedAt);
}
