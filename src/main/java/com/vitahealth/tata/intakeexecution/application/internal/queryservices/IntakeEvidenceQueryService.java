package com.vitahealth.tata.intakeexecution.application.internal.queryservices;

import com.vitahealth.tata.intakeexecution.application.internal.assemblers.IntakeResultFromAggregateAssembler;
import com.vitahealth.tata.intakeexecution.application.result.IntakeResult;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Provides trusted cross-context evidence reads without granting transition locks. */
@Service
@Transactional(readOnly = true)
public class IntakeEvidenceQueryService {
  private final IntakeRepository repository;

  /** Injects the intake evidence repository. */
  public IntakeEvidenceQueryService(IntakeRepository repository) {
    this.repository = repository;
  }

  /** Returns all evidence in an ordered half-open interval for one adult. */
  public List<IntakeResult> findEvidence(String olderAdultId, Instant from, Instant to) {
    if (olderAdultId == null
        || olderAdultId.isBlank()
        || from == null
        || to == null
        || !to.isAfter(from)) {
      throw new IllegalArgumentException("evidence requires an adult and an ordered interval");
    }
    return repository.findAgenda(olderAdultId.trim(), from, to).stream()
        .map(IntakeResultFromAggregateAssembler::toResult)
        .toList();
  }

  /** Returns distinct adults with evidence in the ordered half-open interval. */
  public List<String> findOlderAdultIdsWithIntakes(
      Instant from, Instant to, String after, int limit) {
    if (from == null || to == null || !to.isAfter(from)) {
      throw new IllegalArgumentException("participant discovery requires an ordered interval");
    }
    if (limit < 1 || limit > 100 || (after != null && after.isBlank())) {
      throw new IllegalArgumentException(
          "discovery requires a valid cursor and limit between 1 and 100");
    }
    return List.copyOf(repository.findOlderAdultIdsWithIntakes(from, to, after, limit));
  }

  /** Returns an existing evidence record, with absence represented explicitly. */
  public Optional<IntakeResult> findIntake(String id) {
    if (id == null || id.isBlank()) throw new IllegalArgumentException("intakeId is required");
    return repository.findById(id.trim()).map(IntakeResultFromAggregateAssembler::toResult);
  }
}
