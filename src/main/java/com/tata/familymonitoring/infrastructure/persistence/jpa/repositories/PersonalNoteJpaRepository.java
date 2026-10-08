package com.tata.familymonitoring.infrastructure.persistence.jpa.repositories;

import com.tata.familymonitoring.infrastructure.persistence.jpa.entities.PersonalNotePersistenceEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalNoteJpaRepository
    extends JpaRepository<PersonalNotePersistenceEntity, Long> {
  List<PersonalNotePersistenceEntity> findByOlderAdultIdOrderByRecordedAtDescIdDesc(
      String olderAdultId);
}
