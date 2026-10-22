package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import com.vitahealth.tata.familymonitoring.domain.ports.CareRelationshipPort;
import com.vitahealth.tata.familymonitoring.domain.ports.InitialInventorySourcePort;
import com.vitahealth.tata.familymonitoring.domain.repositories.FamilyStockCatchUpRepository;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.time.Clock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Processes one durable medication cursor step with source and projection in one transaction. */
@Service
public class ProcessFamilyStockCatchUpCommandHandler {
  private final FamilyStockCatchUpRepository work;
  private final IFamilyMonitorRepository monitors;
  private final InitialInventorySourcePort sources;
  private final CareRelationshipPort relationships;
  private final Clock clock;

  /** Injects owned work/history and public source/consent boundaries. */
  public ProcessFamilyStockCatchUpCommandHandler(
      FamilyStockCatchUpRepository work,
      IFamilyMonitorRepository monitors,
      InitialInventorySourcePort sources,
      CareRelationshipPort relationships,
      Clock clock) {
    this.work = work;
    this.monitors = monitors;
    this.sources = sources;
    this.relationships = relationships;
    this.clock = clock;
  }

  /**
   * Returns true only after a due medication step advances; completion and no work return false.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public boolean handle(long monitorId) {
    var found = work.reserve(monitorId);
    if (found.isEmpty()) return false;
    var request = found.orElseThrow();
    if (request.nextAttemptAt().isAfter(clock.instant())) return false;
    var initial = monitors.findById(monitorId);
    if (initial.isEmpty()) {
      work.complete(request);
      return false;
    }
    var identity = initial.orElseThrow();
    var ids =
        sources.findMedicationIdsAfter(identity.getOlderAdultId(), request.afterMedicationId(), 1);
    if (ids.isEmpty()) {
      work.complete(request);
      return false;
    }
    if (ids.size() != 1 || ids.getFirst().compareTo(request.afterMedicationId()) <= 0)
      throw new IllegalStateException("source medication cursor did not advance");
    var medicationId = ids.getFirst();
    var source = sources.reserve(identity.getOlderAdultId(), medicationId);
    var current = monitors.findByIdForUpdate(monitorId);
    if (current.isEmpty()) {
      work.complete(request);
      return false;
    }
    var monitor = current.orElseThrow();
    if (!monitor.getCareLinkId().equals(identity.getCareLinkId())
        || !monitor.getOlderAdultId().equals(identity.getOlderAdultId())
        || !monitor.getFamiliarId().equals(identity.getFamiliarId()))
      throw new IllegalStateException("catch-up relationship identity changed");
    if (!relationships.isRelationshipAuthorized(
        monitor.getCareLinkId(), monitor.getFamiliarId(), monitor.getOlderAdultId())) {
      work.complete(request);
      return false;
    }
    if (source.isPresent()
        && source.get().stock().isPresent()
        && monitor.projectInventoryStock(
            source.get().stock().orElseThrow(), source.get().medicationName()))
      monitors.save(monitor);
    if (!work.replace(request, request.advance(medicationId, clock.instant())))
      throw new IllegalStateException("reserved catch-up progress changed unexpectedly");
    return true;
  }
}
