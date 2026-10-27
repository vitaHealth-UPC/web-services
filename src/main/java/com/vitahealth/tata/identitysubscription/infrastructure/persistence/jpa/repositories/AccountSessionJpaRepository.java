package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import java.time.Instant;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Persists credential digests and their revocation/expiry evidence without storing raw tokens. */
public interface AccountSessionJpaRepository
    extends JpaRepository<AccountSessionPersistenceEntity, String> {
  /** Looks up the persisted evidence for a credential digest. */
  Optional<AccountSessionPersistenceEntity> findByTokenHash(String hash);

  /** Revokes every record matching the unique credential digest. */
  void deleteByTokenHash(String hash);

  /** Removes adult and setup credentials scoped to a withdrawn relationship. */
  void deleteByCareLinkId(String careLinkId);

  /** Removes password-account sessions, including legacy rows without a role. */
  @Modifying(flushAutomatically = true)
  @Query(
      "delete from AccountSessionPersistenceEntity s where s.accountId = :accountId and (s.role ="
          + " 'CAREGIVER' or s.role is null)")
  int revokeCaregiverSessions(@Param("accountId") String accountId);

  /** Finds an unexpired credential; callers still verify current ownership and consent. */
  Optional<AccountSessionPersistenceEntity> findByTokenHashAndExpiresAtAfter(
      String hash, Instant now);
}
