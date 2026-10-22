package com.vitahealth.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.familymonitoring.application.internal.queryservices.FamilyMonitorSelectionService;
import com.vitahealth.tata.familymonitoring.domain.exceptions.FamilyMonitorNotFoundException;
import com.vitahealth.tata.familymonitoring.domain.model.aggregates.FamilyMonitor;
import com.vitahealth.tata.familymonitoring.domain.ports.CareRelationshipPort;
import com.vitahealth.tata.familymonitoring.domain.repositories.IFamilyMonitorRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** Protects current-link selection against revoked history and ambiguous active relationships. */
class FamilyMonitorSelectionServiceTest {
  private final IFamilyMonitorRepository repository = mock(IFamilyMonitorRepository.class);
  private final CareRelationshipPort relationships = mock(CareRelationshipPort.class);
  private final FamilyMonitorSelectionService selection =
      new FamilyMonitorSelectionService(repository, relationships);
  private final FamilyMonitor old = new FamilyMonitor("old-link", "adult", "caregiver");
  private final FamilyMonitor current = new FamilyMonitor("new-link", "adult", "caregiver");

  @BeforeEach
  void participantHistory() {
    when(repository.findIdsByParticipantsAfter("adult", "caregiver", 0, 100))
        .thenReturn(List.of(1L, 2L));
    when(repository.findIdsByParticipantsAfter("adult", "caregiver", 2, 100)).thenReturn(List.of());
    when(repository.findById(1L)).thenReturn(Optional.of(old));
    when(repository.findById(2L)).thenReturn(Optional.of(current));
  }

  @Test
  void selectsTheConsentedLinkRatherThanTheFirstHistoricalMatch() {
    when(relationships.isRelationshipAuthorized("new-link", "caregiver", "adult")).thenReturn(true);
    assertSame(current, selection.requireCurrent("adult", "caregiver"));
  }

  @Test
  void refusesToChooseBetweenTwoActiveFollowUps() {
    when(relationships.isRelationshipAuthorized("old-link", "caregiver", "adult")).thenReturn(true);
    when(relationships.isRelationshipAuthorized("new-link", "caregiver", "adult")).thenReturn(true);
    assertThrows(IllegalStateException.class, () -> selection.requireCurrent("adult", "caregiver"));
  }

  @Test
  void revokedHistoryDoesNotActAsAFallbackWhenNoCurrentConsentExists() {
    assertThrows(
        FamilyMonitorNotFoundException.class, () -> selection.requireCurrent("adult", "caregiver"));
  }
}
