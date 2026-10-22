package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.PersonalNotePersistenceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Selects private reminder rows using exact owner scope and deterministic reverse ordering. */
public interface PersonalNoteJpaRepository
    extends JpaRepository<PersonalNotePersistenceEntity, Long> {
  /** Breaks equal recording timestamps by stored identity, newest first. */
  List<PersonalNotePersistenceEntity> findByOlderAdultIdOrderByRecordedAtDescIdDesc(
      String olderAdultId);
}
