package com.tata.familymonitoring.application.internal.commandservices;

import com.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;
import com.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CreateCaregiverNoteCommandHandler {

  private final IFamilyMonitorRepository repository;

  public CreateCaregiverNoteCommandHandler(IFamilyMonitorRepository repository) {
    this.repository = repository;
  }

  @Transactional
  public CaregiverNote handle(CreateCaregiverNoteCommand command) {
    FamilyMonitor monitor = repository.findByOlderAdultId(command.olderAdultId())
        .orElseThrow(() -> new FamilyMonitorNotFoundException(command.olderAdultId()));
    monitor.addNote(command.text(), command.familiarId(), Instant.now());
    return repository.save(monitor).latestNote();
  }
}
