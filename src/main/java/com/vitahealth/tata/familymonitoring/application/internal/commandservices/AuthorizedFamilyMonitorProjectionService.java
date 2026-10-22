package com.vitahealth.tata.familymonitoring.application.internal.commandservices;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.ports.CareRelationshipPort;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Applies source projections to reserved, currently consented relationship follow-ups. */
@Service
public class AuthorizedFamilyMonitorProjectionService {
  private final IFamilyMonitorRepository repository;
  private final CareRelationshipPort relationships;

  /** Injects owned follow-up storage and the exact relationship authorization boundary. */
  public AuthorizedFamilyMonitorProjectionService(
      IFamilyMonitorRepository repository, CareRelationshipPort relationships) {
    this.repository = repository;
    this.relationships = relationships;
  }

  /** Scans bounded reference pages and preserves the source command's rollback boundary. */
  @Transactional(propagation = Propagation.MANDATORY)
  public void apply(String olderAdultId, Consumer<FamilyMonitor> operation) {
    Objects.requireNonNull(operation, "projection operation");
    applyIfChanged(
        olderAdultId,
        monitor -> {
          operation.accept(monitor);
          return true;
        });
  }

  /**
   * Saves only changed projections so duplicate or obsolete source evidence leaves audit intact.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public void applyIfChanged(String olderAdultId, Predicate<FamilyMonitor> operation) {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    Objects.requireNonNull(operation, "projection operation");
    long cursor = 0;
    while (true) {
      var ids = repository.findIdsByOlderAdultIdAfter(olderAdultId, cursor, 100);
      if (ids.isEmpty()) return;
      for (Long id : ids) {
        if (id == null || id <= cursor)
          throw new IllegalStateException("monitor cursor did not advance");
        cursor = id;
        var found = repository.findByIdForUpdate(id);
        if (found.isEmpty()) continue;
        var monitor = found.orElseThrow();
        if (!monitor.getOlderAdultId().equals(olderAdultId))
          throw new IllegalStateException("monitor source owner changed");
        if (!relationships.isRelationshipAuthorized(
            monitor.getCareLinkId(), monitor.getFamiliarId(), olderAdultId)) continue;
        if (operation.test(monitor)) repository.save(monitor);
      }
    }
  }
}
