package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.adapters;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.assemblers.PersonalNotePersistenceAssembler;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.repositories.PersonalNoteJpaRepository;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Inserts immutable private notes and restores owner-scoped history. */
@Repository
public class PersonalNoteRepositoryAdapter implements PersonalNoteRepository {
  private final PersonalNoteJpaRepository repository;

  private final EntityManager entities;

  /** Injects owner queries and the persistence context for insert-only writes. */
  public PersonalNoteRepositoryAdapter(
      PersonalNoteJpaRepository repository, EntityManager entities) {
    this.entities = entities;
    this.repository = repository;
  }

  /** Inserts a new identity without allowing replacement of existing owner evidence. */
  @Override
  @Transactional
  public PersonalNote append(PersonalNote note) {
    if (note.id() != null)
      throw new IllegalArgumentException("only new personal notes can be appended");
    var entity = PersonalNotePersistenceAssembler.toEntity(note);
    entities.persist(entity);
    entities.flush();
    return PersonalNotePersistenceAssembler.toDomain(entity);
  }

  /** Reads a single owner in deterministic reverse chronological order. */
  @Override
  @Transactional(readOnly = true)
  public List<PersonalNote> findByOwner(String owner) {
    owner = requireText(owner, "olderAdultId");
    return repository.findByOlderAdultIdOrderByRecordedAtDescIdDesc(owner).stream()
        .map(PersonalNotePersistenceAssembler::toDomain)
        .toList();
  }
}
