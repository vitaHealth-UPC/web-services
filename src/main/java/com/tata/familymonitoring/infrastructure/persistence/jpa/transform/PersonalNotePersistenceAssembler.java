package com.tata.familymonitoring.infrastructure.persistence.jpa.transform;

import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.tata.familymonitoring.infrastructure.persistence.jpa.entities.PersonalNotePersistenceEntity;

public final class PersonalNotePersistenceAssembler {
  private PersonalNotePersistenceAssembler() {}

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
