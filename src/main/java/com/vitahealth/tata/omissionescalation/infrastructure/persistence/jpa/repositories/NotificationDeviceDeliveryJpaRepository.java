package com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.omissionescalation.infrastructure.notifications.queue.NotificationDeliveryState;
import com.vitahealth.tata.omissionescalation.infrastructure.persistence.jpa.entities.NotificationDeviceDeliveryPersistenceEntity;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Persists independent installation deliveries with bounded work selection and explicit row locks.
 */
public interface NotificationDeviceDeliveryJpaRepository
    extends JpaRepository<NotificationDeviceDeliveryPersistenceEntity, String> {
  /** Detects a repeated fanout selection without duplicating the device delivery. */
  Optional<NotificationDeviceDeliveryPersistenceEntity> findByIntent_IdAndInstallationId(
      String intentId, String installationId);

  /** Reserves attempt evidence before changing lease or outcome. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select delivery from NotificationDeviceDeliveryPersistenceEntity delivery where delivery.id"
          + " = :id")
  Optional<NotificationDeviceDeliveryPersistenceEntity> findForUpdate(@Param("id") String id);

  /** Selects IDs only; an expired worker reservation becomes eligible for recovery. */
  @Query(
      "select delivery.id from NotificationDeviceDeliveryPersistenceEntity delivery where"
          + " delivery.state = :ready and delivery.nextAttemptAt <= :now and (delivery.leaseUntil"
          + " is null or delivery.leaseUntil <= :now) order by delivery.nextAttemptAt, delivery.id")
  List<String> findDueIds(
      @Param("ready") NotificationDeliveryState ready, @Param("now") Instant now, Pageable page);

  /** Reports whether any installation work exists before finishing a recipient scan. */
  boolean existsByIntent_Id(String intentId);

  /** Deletes owned technical work when its intention is explicitly removed. */
  void deleteAllByIntent_Id(String intentId);

  /** Reports remaining work without reconstructing device rows or their session references. */
  boolean existsByIntent_IdAndState(String intentId, NotificationDeliveryState state);

  /** Returns the final actual provider acceptance time; cancellation never contributes evidence. */
  @Query(
      "select max(delivery.acceptedAt) from NotificationDeviceDeliveryPersistenceEntity delivery"
          + " where delivery.intent.id = :intentId and delivery.state = :accepted")
  Optional<Instant> findLatestAcceptance(
      @Param("intentId") String intentId, @Param("accepted") NotificationDeliveryState accepted);

  /** Scans due device IDs in stable keyset order with expired-lease recovery. */
  @Query(
      "select delivery.id from NotificationDeviceDeliveryPersistenceEntity delivery where"
          + " delivery.id > :afterId and delivery.state = :ready and delivery.nextAttemptAt <= :now"
          + " and (delivery.leaseUntil is null or delivery.leaseUntil <= :now) order by"
          + " delivery.id")
  List<String> findDueIdsAfter(
      @Param("afterId") String afterId,
      @Param("ready") NotificationDeliveryState ready,
      @Param("now") Instant now,
      Pageable page);
}
