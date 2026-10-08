package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.application.commandservices.PersonalNoteCommandService;
import com.tata.familymonitoring.application.queryservices.PersonalNoteQueryService;
import com.tata.familymonitoring.domain.model.commands.CreatePersonalNoteCommand;
import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PersonalNoteService implements PersonalNoteCommandService, PersonalNoteQueryService {
  private final PersonalNoteRepository repository;
  private final Clock clock;

  public PersonalNoteService(PersonalNoteRepository repository, Clock clock) {
    this.repository = repository;
    this.clock = clock;
  }

  @Transactional
  public PersonalNote handle(CreatePersonalNoteCommand command) {
    return repository.save(
        new PersonalNote(
            null,
            command.olderAdultId(),
            command.title(),
            command.text(),
            command.category(),
            Instant.now(clock)));
  }

  @Transactional(readOnly = true)
  public List<PersonalNote> list(String olderAdultId) {
    return List.copyOf(repository.findByOwner(olderAdultId));
  }
}
