package com.vitahealth.tata.familymonitoring.application.internal.queryservices;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.ports.CareRelationshipPort;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Selects exactly one currently consented follow-up without exposing revoked relationship history.
 */
@Service
public class FamilyMonitorSelectionService {
  private final IFamilyMonitorRepository repository;
  private final CareRelationshipPort relationships;

  /** Injects context-owned history selection and exact Care Link authorization. */
  public FamilyMonitorSelectionService(
      IFamilyMonitorRepository repository, CareRelationshipPort relationships) {
    this.repository = repository;
    this.relationships = relationships;
  }

  /** Returns the unique active relationship snapshot for a caregiver-adult read. */
  @Transactional(readOnly = true)
  public FamilyMonitor requireCurrent(String olderAdultId, String caregiverId) {
    return select(olderAdultId, caregiverId, false);
  }

  /**
   * Reserves current follow-up inside the caller's write transaction and rechecks exact consent.
   */
  @Transactional(propagation = Propagation.MANDATORY)
  public FamilyMonitor requireCurrentForUpdate(String olderAdultId, String caregiverId) {
    return select(olderAdultId, caregiverId, true);
  }

  private FamilyMonitor select(String olderAdultId, String caregiverId, boolean reserve) {
    olderAdultId = requireText(olderAdultId, "olderAdultId");
    caregiverId = requireText(caregiverId, "caregiverId");
    FamilyMonitor selected = null;
    long cursor = 0;
    while (true) {
      var ids = repository.findIdsByParticipantsAfter(olderAdultId, caregiverId, cursor, 100);
      if (ids.isEmpty()) {
        if (selected == null) throw new FamilyMonitorNotFoundException(olderAdultId);
        return selected;
      }
      for (Long id : ids) {
        if (id == null || id <= cursor)
          throw new IllegalStateException("monitor selection cursor did not advance");
        cursor = id;
        var found = reserve ? repository.findByIdForUpdate(id) : repository.findById(id);
        if (found.isEmpty()) continue;
        var monitor = found.orElseThrow();
        if (!olderAdultId.equals(monitor.getOlderAdultId())
            || !caregiverId.equals(monitor.getFamiliarId()))
          throw new IllegalStateException("monitor participant scope changed");
        if (!relationships.isRelationshipAuthorized(
            monitor.getCareLinkId(), caregiverId, olderAdultId)) continue;
        if (selected != null) throw new IllegalStateException("multiple active family follow-ups");
        selected = monitor;
      }
    }
  }
}
