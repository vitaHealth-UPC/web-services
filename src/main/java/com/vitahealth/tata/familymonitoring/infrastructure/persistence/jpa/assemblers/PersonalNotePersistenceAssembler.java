package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.assemblers;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities.PersonalNotePersistenceEntity;

/** Maps immutable note evidence without changing its stored text or timestamps. */
public final class PersonalNotePersistenceAssembler {
  private PersonalNotePersistenceAssembler() {}

  /** Copies a new note into a transient persistence entity. */
  public static PersonalNotePersistenceEntity toEntity(PersonalNote note) {
    var entity = new PersonalNotePersistenceEntity();
    entity.setId(note.id());
    entity.setOlderAdultId(note.olderAdultId());
    entity.setTitle(note.title());
    entity.setText(note.text());
    entity.setCategory(note.category().name());
    entity.setRecordedAt(note.recordedAt());
    return entity;
  }

  /** Restores original stored evidence through domain validation. */
  public static PersonalNote toDomain(PersonalNotePersistenceEntity entity) {
    return new PersonalNote(
        entity.getId(),
        entity.getOlderAdultId(),
        entity.getTitle(),
        entity.getText(),
        PersonalNote.Category.valueOf(entity.getCategory()),
        entity.getRecordedAt());
  }
}
