package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.FamilyMonitorPersistenceEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Queries stored identities and exact relationships without collapsing historical follow-ups. */
public interface FamilyMonitorJpaRepository
    extends JpaRepository<FamilyMonitorPersistenceEntity, Long> {

  /**
   * Selects participant-scoped references while leaving current consent decisions to application.
   */
  @Query(
      "select m.id from FamilyMonitorPersistenceEntity m where m.olderAdultId = :adult and"
          + " m.familiarId = :caregiver and m.id > :after order by m.id")
  List<Long> findIdsByParticipantsAfter(
      @Param("adult") String adult,
      @Param("caregiver") String caregiver,
      @Param("after") long after,
      Pageable page);

  /** Selects scalar references without materializing every owned history. */
  @Query(
      "select m.id from FamilyMonitorPersistenceEntity m where m.olderAdultId = :adult and m.id >"
          + " :after order by m.id")
  List<Long> findIdsByOlderAdultIdAfter(
      @Param("adult") String adult, @Param("after") long after, Pageable page);

  /** Selects the single projection owned by the exact care relationship. */
  Optional<FamilyMonitorPersistenceEntity> findByCareLinkId(String careLinkId);
}
