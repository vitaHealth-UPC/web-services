package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.model.commands.InitializeFamilyMonitorCommand;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.FamilyStockCatchUp;
import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Initializes relationship follow-up in the same transaction as consent confirmation. */
@Service
public class InitializeFamilyMonitorCommandHandler {
  private final IFamilyMonitorRepository repository;

  private final FamilyStockCatchUpRepository catchUps;
  private final Clock clock;

  /** Injects owned follow-up and durable initial stock work storage. */
  public InitializeFamilyMonitorCommandHandler(
      IFamilyMonitorRepository repository, FamilyStockCatchUpRepository catchUps, Clock clock) {
    this.repository = repository;
    this.catchUps = catchUps;
    this.clock = clock;
  }

  /** Creates missing follow-up once; an existing relationship retains its stored history. */
  @Transactional
  public void handle(InitializeFamilyMonitorCommand command) {
    var existing = repository.findByCareLinkId(command.careLinkId());
    if (existing.isPresent()) {
      var monitor = existing.orElseThrow();
      if (!monitor.getOlderAdultId().equals(command.olderAdultId())
          || !monitor.getFamiliarId().equals(command.caregiverId()))
        throw new IllegalStateException(
            "confirmed relationship participants conflict with follow-up");
      return;
    }
    var saved =
        repository.save(
            new FamilyMonitor(command.careLinkId(), command.olderAdultId(), command.caregiverId()));
    catchUps.enqueue(FamilyStockCatchUp.start(saved.getId(), clock.instant()));
  }
}
