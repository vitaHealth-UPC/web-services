package com.tata.omissionescalation.infrastructure.persistence.jpa.repositories;

import com.tata.omissionescalation.infrastructure.persistence.jpa.entities.OmissionCasePersistenceEntity;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OmissionCaseJpaRepository extends JpaRepository<OmissionCasePersistenceEntity, Long> {

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select c from OmissionCasePersistenceEntity c where c.id = :id")
  Optional<OmissionCasePersistenceEntity> findByIdForUpdate(@org.springframework.data.repository.query.Param("id") Long id);

  Optional<OmissionCasePersistenceEntity> findByIntakeId(String intakeId);

  List<OmissionCasePersistenceEntity> findByStatusAndGracePeriodEndsAtLessThanEqual(
      OmissionCaseStatus status, Instant now);

  List<OmissionCasePersistenceEntity> findByStatusIn(Collection<OmissionCaseStatus> statuses);
}
