package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.vitahealth.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersonalNotesPersistenceTest {
  @Autowired PersonalNoteRepository repository;
  @Autowired EntityManager entities;
  @Autowired JdbcTemplate jdbc;

  @Test
  void storedNotesCannotBeOverwrittenOrMovedToAnotherOwner() {
    var original =
        repository.append(
            new PersonalNote(
                null,
                "private-original",
                "Original",
                "Evidence",
                PersonalNote.Category.ROUTINE,
                Instant.parse("2026-10-19T12:00:00Z")));
    var replacement =
        new PersonalNote(
            original.id(),
            "private-foreign",
            "Changed",
            "Changed",
            PersonalNote.Category.MEDICATION,
            original.recordedAt());
    var failure =
        assertThrows(
            InvalidDataAccessApiUsageException.class, () -> repository.append(replacement));
    assertInstanceOf(IllegalArgumentException.class, failure.getMostSpecificCause());
    entities.clear();
    assertEquals(List.of(original), repository.findByOwner("private-original"));
    assertTrue(repository.findByOwner("private-foreign").isEmpty());
  }

  @Test
  void readingStoredEvidenceDoesNotTrimItsOriginalText() {
    var original =
        repository.append(
            new PersonalNote(
                null,
                "private-verbatim",
                "Original",
                "Evidence",
                PersonalNote.Category.ROUTINE,
                Instant.parse("2026-10-19T12:00:00Z")));
    jdbc.update(
        "update personal_notes set title = ?, text = ? where id = ?",
        " Original title ",
        " Original text ",
        original.id());
    entities.clear();
    var restored = repository.findByOwner("private-verbatim").getFirst();
    assertEquals(" Original title ", restored.title());
    assertEquals(" Original text ", restored.text());
    assertEquals(original.id(), restored.id());
    assertEquals(original.recordedAt(), restored.recordedAt());
  }

  @Test
  void persistsAndReadsOnlyOwnerWithDeterministicOrder() {
    var date = Instant.parse("2026-10-08T13:00:00Z");
    var first =
        repository.append(
            new PersonalNote(
                null, "personal-test-a", "Uno", "Agua", PersonalNote.Category.MEDICATION, date));
    var second =
        repository.append(
            new PersonalNote(
                null, "personal-test-a", "Dos", "Rutina", PersonalNote.Category.ROUTINE, date));
    repository.append(
        new PersonalNote(
            null, "personal-test-b", "Privada", "Otro", PersonalNote.Category.ROUTINE, date));
    var notes = repository.findByOwner("personal-test-a");
    assertEquals(2, notes.size());
    assertEquals(second, notes.get(0));
    assertEquals(first, notes.get(1));
  }
}
