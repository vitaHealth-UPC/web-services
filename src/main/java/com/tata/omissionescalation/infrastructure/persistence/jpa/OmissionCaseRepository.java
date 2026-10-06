package com.tata.omissionescalation.infrastructure.persistence.jpa;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class OmissionCaseRepository implements IOmissionCaseRepository {

  private final OmissionCaseJpaRepository jpaRepository;

  public OmissionCaseRepository(OmissionCaseJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public OmissionCase save(OmissionCase omissionCase) {
    return jpaRepository.saveAndFlush(omissionCase);
  }

  @Override
  public Optional<OmissionCase> findById(Long id) {
    return jpaRepository.findById(id);
  }

  @Override
  public Optional<OmissionCase> findByIntakeId(Long intakeId) {
    return jpaRepository.findByIntakeId(intakeId);
  }

  @Override
  public List<OmissionCase> findExpiredPending(Instant now) {
    return jpaRepository.findByStatusAndGracePeriodEndsAtLessThanEqual(
        OmissionCaseStatus.PENDING, now);
  }

  @Override
  public List<OmissionCase> findOmittedOrEscalated() {
    return jpaRepository.findByStatusIn(
        List.of(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED));
  }
}
