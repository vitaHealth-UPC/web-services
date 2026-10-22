package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.application.commandservices.CreateCaregiverNoteCommandService;
import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.application.result.CaregiverNoteResult;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CreateCaregiverNoteCommand;
import com.vitahealth.tata.familymonitoring.domain.model.entities.CaregiverNote;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Clock;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Registers intervention notes in the author-owned follow-up. */
@Service
public class CreateCaregiverNoteCommandHandler implements CreateCaregiverNoteCommandService {

  private final IFamilyMonitorRepository repository;
  private final FamilyMonitorSelectionService selection;

  private final Clock clock;

  /** Injects scoped storage and the common application clock. */
  public CreateCaregiverNoteCommandHandler(
      IFamilyMonitorRepository repository, Clock clock, FamilyMonitorSelectionService selection) {
    this.repository = repository;
    this.selection = selection;
    this.clock = clock;
  }

  /** Persists the note with its actual author and application occurrence time. */
  @Override
  @Transactional
  public CaregiverNoteResult handle(CreateCaregiverNoteCommand command) {
    FamilyMonitor monitor =
        selection.requireCurrentForUpdate(command.olderAdultId(), command.familiarId());
    var previousIds =
        monitor.getNotes().stream().map(CaregiverNote::getId).collect(Collectors.toSet());
    monitor.addNote(command.text(), command.familiarId(), clock.instant());
    var created =
        repository.save(monitor).getNotes().stream()
            .filter(note -> !previousIds.contains(note.getId()))
            .toList();
    if (created.size() != 1)
      throw new IllegalStateException("created follow-up note identity is ambiguous");
    return CaregiverNoteResult.from(created.getFirst());
  }
}
