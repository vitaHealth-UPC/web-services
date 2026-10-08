package com.tata.familymonitoring.infrastructure.persistence.jpa.repositories;

import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import com.tata.familymonitoring.infrastructure.persistence.jpa.transform.PersonalNotePersistenceAssembler;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class PersonalNoteRepositoryAdapter implements PersonalNoteRepository {
  private final PersonalNoteJpaRepository repository;

  public PersonalNoteRepositoryAdapter(PersonalNoteJpaRepository repository) {
    this.repository = repository;
  }

  public PersonalNote save(PersonalNote note) {
    return PersonalNotePersistenceAssembler.toDomain(
        repository.save(PersonalNotePersistenceAssembler.toEntity(note)));
  }

  public List<PersonalNote> findByOwner(String owner) {
    return repository.findByOlderAdultIdOrderByRecordedAtDescIdDesc(owner).stream()
        .map(PersonalNotePersistenceAssembler::toDomain)
        .toList();
  }
}
