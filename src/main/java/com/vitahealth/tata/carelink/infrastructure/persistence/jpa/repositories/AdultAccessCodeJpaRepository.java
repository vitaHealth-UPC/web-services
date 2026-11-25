package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.AdultAccessCodePersistenceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;

/** Spring Data access to adult sign-in codes. */
public interface AdultAccessCodeJpaRepository
    extends JpaRepository<AdultAccessCodePersistenceEntity, String> {
  /** Reserves a code row for redemption. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<AdultAccessCodePersistenceEntity> findByCode(String code);

  /** Checks value uniqueness before issuing. */
  boolean existsByCode(String code);

  /** Drops unused codes of a relationship. */
  @Modifying(flushAutomatically = true)
  void deleteByCareLinkIdAndUsedAtIsNull(String careLinkId);

  /** Drops unused codes of one kind for a relationship. */
  @Modifying(flushAutomatically = true)
  void deleteByCareLinkIdAndKindAndUsedAtIsNull(String careLinkId, String kind);
}
