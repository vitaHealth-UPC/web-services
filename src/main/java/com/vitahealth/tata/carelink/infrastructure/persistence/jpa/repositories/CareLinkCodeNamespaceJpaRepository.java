package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkCodeNamespaceEntity;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Initializes and reserves issuance coordination using a context-owned database row. */
public interface CareLinkCodeNamespaceJpaRepository
    extends JpaRepository<CareLinkCodeNamespaceEntity, String> {
  /** Reads the existing namespace identity during initialization. */
  Optional<CareLinkCodeNamespaceEntity> findByNamespace(String namespace);

  /** Serializes issuance across application instances sharing the same database. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select namespace from CareLinkCodeNamespaceEntity namespace where namespace.namespace ="
          + " :name")
  Optional<CareLinkCodeNamespaceEntity> findByNamespaceForUpdate(@Param("name") String namespace);
}
