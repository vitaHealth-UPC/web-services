package com.tata.omissionescalation.infrastructure.persistence.jpa.adapters;

import com.tata.omissionescalation.infrastructure.persistence.jpa.repositories.OmissionCaseJpaRepository;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import com.tata.omissionescalation.infrastructure.persistence.jpa.assemblers.PersistenceAssembler;
import org.springframework.stereotype.Repository;

@Repository
@org.springframework.transaction.annotation.Transactional
public class OmissionCaseRepository implements IOmissionCaseRepository {

  private final OmissionCaseJpaRepository jpaRepository;

  public OmissionCaseRepository(OmissionCaseJpaRepository jpaRepository) {
    this.jpaRepository = jpaRepository;
  }

  @Override
  public OmissionCase save(OmissionCase omissionCase) {
    return PersistenceAssembler.toDomain(jpaRepository.saveAndFlush(PersistenceAssembler.toEntity(omissionCase)));
  }

  @Override
  public Optional<OmissionCase> findById(Long id) {
    return jpaRepository.findById(id).map(PersistenceAssembler::toDomain);
  }

  @Override
  public Optional<OmissionCase> findByIntakeId(String intakeId) {
    return jpaRepository.findByIntakeId(intakeId).map(PersistenceAssembler::toDomain);
  }

  @Override
  public List<OmissionCase> findExpiredPending(Instant now) {
    return jpaRepository.findByStatusAndGracePeriodEndsAtLessThanEqual(
        OmissionCaseStatus.PENDING, now).stream().map(PersistenceAssembler::toDomain).toList();
  }

  @Override
  public List<OmissionCase> findOmittedOrEscalated() {
    return jpaRepository.findByStatusIn(
        List.of(OmissionCaseStatus.OMITTED, OmissionCaseStatus.ESCALATED)).stream().map(PersistenceAssembler::toDomain).toList();
  }
}
