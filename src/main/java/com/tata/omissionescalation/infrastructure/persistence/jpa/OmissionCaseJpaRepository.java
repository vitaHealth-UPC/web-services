package com.tata.omissionescalation.infrastructure.persistence.jpa;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OmissionCaseJpaRepository extends JpaRepository<OmissionCase, Long> {
  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select c from OmissionCase c where c.id = :id")
  Optional<OmissionCase> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select c from OmissionCase c where c.intakeId = :id")
  Optional<OmissionCase> findByIntakeIdForUpdate(@org.springframework.data.repository.query.Param("id") String id);

  Optional<OmissionCase> findByIntakeId(String intakeId);

  List<OmissionCase> findByStatusAndGracePeriodEndsAtLessThanEqual(
      OmissionCaseStatus status, Instant now);

  List<OmissionCase> findByStatusIn(Collection<OmissionCaseStatus> statuses);
}
