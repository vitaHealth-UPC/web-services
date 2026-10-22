package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Loads relationship rows and holds database reservations for lifecycle commands. */
public interface CareLinkJpaRepository extends JpaRepository<CareLinkPersistenceEntity, String> {
  /** Reads confirmed relationships in deterministic display order. */
  List<CareLinkPersistenceEntity> findByCaregiverIdAndStatusOrderByConfirmedAtDescIdAsc(
      String caregiverId, CareLinkStatus status);

  /** Reads the unique current reservation before considering historical code reuse. */
  Optional<CareLinkPersistenceEntity> findByReservedLinkingCode(String code);

  /** Reports whether a candidate currently has an active reservation. */
  boolean existsByReservedLinkingCode(String code);

  /** Reads the latest historical code evidence when no active reservation exists. */
  Optional<CareLinkPersistenceEntity> findFirstByLinkingCodeOrderByCodeExpiresAtDescIdAsc(
      String code);

  /** Frees expired pending code and pair reservations without changing business history. */
  @Modifying
  @Query(
      "update CareLinkPersistenceEntity link set link.reservedLinkingCode = null,"
          + " link.reservedCaregiverId = null, link.reservedOlderAdultId = null where"
          + " link.status = :pending and link.codeExpiresAt <= :now")
  int releaseExpiredCodeReservations(
      @Param("pending") CareLinkStatus pending, @Param("now") Instant now);

  /** Restores active pair reservations and lets the named constraint reject ambiguous history. */
  @Modifying
  @Query(
      "update CareLinkPersistenceEntity link set link.reservedCaregiverId = link.caregiverId,"
          + " link.reservedOlderAdultId = link.olderAdultId where (link.reservedCaregiverId is null"
          + " or link.reservedOlderAdultId is null)"
          + " and ((link.status = :pending and link.codeExpiresAt > :now) or link.status in"
          + " :active)")
  int reserveUnclaimedMemberPairs(
      @Param("pending") CareLinkStatus pending,
      @Param("active") Collection<CareLinkStatus> active,
      @Param("now") Instant now);

  /** Reserves open member-pair evidence while issuance holds the shared namespace lock. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select link from CareLinkPersistenceEntity link where link.caregiverId = :caregiver and"
          + " link.olderAdultId = :adult and ((link.status = :pending and link.codeExpiresAt >"
          + " :now) or link.status in :active) order by link.createdAt, link.id")
  List<CareLinkPersistenceEntity> findOpenMemberPairForUpdate(
      @Param("caregiver") String caregiver,
      @Param("adult") String adult,
      @Param("pending") CareLinkStatus pending,
      @Param("active") Collection<CareLinkStatus> active,
      @Param("now") Instant now);

  /** Backfills existing unused live code evidence, preserving all historical rows. */
  @Modifying
  @Query(
      "update CareLinkPersistenceEntity link set link.reservedLinkingCode = link.linkingCode where"
          + " link.reservedLinkingCode is null and link.status = :pending and link.codeUsedAt is"
          + " null and link.codeExpiresAt > :now")
  int reserveUnclaimedLiveCodes(
      @Param("pending") CareLinkStatus pending, @Param("now") Instant now);

  /** Locks the current row before applying a consent or relationship transition. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select link from CareLinkPersistenceEntity link where link.id = :id")
  Optional<CareLinkPersistenceEntity> findByIdForUpdate(@Param("id") String id);

  /** Locks code evidence before attempting its single acceptance transition. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select link from CareLinkPersistenceEntity link where link.reservedLinkingCode = :code")
  Optional<CareLinkPersistenceEntity> findByCodeForUpdate(@Param("code") String code);

  /** Reserves the latest historical evidence when a used or expired code has no active owner. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select link from CareLinkPersistenceEntity link where link.linkingCode = :code order by"
          + " link.codeExpiresAt desc, link.id asc")
  List<CareLinkPersistenceEntity> findHistoricalCodeForUpdate(
      @Param("code") String code, Pageable page);

  /** Finds an existing confirmed relationship for the specified member pair. */
  Optional<CareLinkPersistenceEntity> findFirstByCaregiverIdAndOlderAdultIdAndStatus(
      String caregiverId, String olderAdultId, CareLinkStatus status);

  /** Selects credential-free caregiver candidates in stable keyset order. */
  @Query(
      "select distinct link.caregiverId from CareLinkPersistenceEntity link where link.olderAdultId"
          + " = :adult and link.status = :confirmed and link.consentGranted = true and"
          + " link.confirmedAt is not null and link.caregiverId > :afterId order by"
          + " link.caregiverId")
  List<String> findAuthorizedCaregiverIdsAfter(
      @Param("adult") String olderAdultId,
      @Param("confirmed") CareLinkStatus confirmed,
      @Param("afterId") String afterId,
      Pageable page);
}
