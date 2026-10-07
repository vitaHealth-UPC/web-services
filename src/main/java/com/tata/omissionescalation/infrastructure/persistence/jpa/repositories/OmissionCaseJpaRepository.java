package com.tata.omissionescalation.infrastructure.persistence.jpa.repositories;

import com.tata.omissionescalation.infrastructure.persistence.jpa.entities.OmissionCasePersistenceEntity;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OmissionCaseJpaRepository extends JpaRepository<OmissionCasePersistenceEntity, Long> {

  Optional<OmissionCasePersistenceEntity> findByIntakeId(String intakeId);

  List<OmissionCasePersistenceEntity> findByStatusAndGracePeriodEndsAtLessThanEqual(
      OmissionCaseStatus status, Instant now);

  List<OmissionCasePersistenceEntity> findByStatusIn(Collection<OmissionCaseStatus> statuses);
}
