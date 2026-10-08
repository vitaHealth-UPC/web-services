package com.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.*;

import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PersonalNotesPersistenceTest {
  @Autowired PersonalNoteRepository repository;

  @Test
  void persistsAndReadsOnlyOwnerWithDeterministicOrder() {
    var date = Instant.parse("2026-10-08T13:00:00Z");
    var first =
        repository.save(
            new PersonalNote(
                null, "personal-test-a", "Uno", "Agua", PersonalNote.Category.MEDICATION, date));
    var second =
        repository.save(
            new PersonalNote(
                null, "personal-test-a", "Dos", "Rutina", PersonalNote.Category.ROUTINE, date));
    repository.save(
        new PersonalNote(
            null, "personal-test-b", "Privada", "Otro", PersonalNote.Category.ROUTINE, date));
    var notes = repository.findByOwner("personal-test-a");
    assertEquals(2, notes.size());
    assertEquals(second, notes.get(0));
    assertEquals(first, notes.get(1));
  }
}
