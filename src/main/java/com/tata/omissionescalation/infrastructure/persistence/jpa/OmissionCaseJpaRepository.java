package com.tata.omissionescalation.infrastructure.persistence.jpa;

import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OmissionCaseJpaRepository extends JpaRepository<OmissionCase, Long> {

  Optional<OmissionCase> findByIntakeId(Long intakeId);

  List<OmissionCase> findByStatusAndGracePeriodEndsAtLessThanEqual(
      OmissionCaseStatus status, Instant now);

  List<OmissionCase> findByStatusIn(Collection<OmissionCaseStatus> statuses);
}
