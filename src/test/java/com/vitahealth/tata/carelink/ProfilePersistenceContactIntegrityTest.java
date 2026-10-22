package com.vitahealth.tata.carelink;

import static org.junit.jupiter.api.Assertions.*;

import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.assemblers.OlderAdultProfilePersistenceAssembler;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.OlderAdultProfilePersistenceEntity;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Ensures incomplete stored contact evidence cannot silently disappear during domain hydration. */
class ProfilePersistenceContactIntegrityTest {
  private OlderAdultProfilePersistenceEntity profile(
      String name, String relationship, String phone) {
    var now = Instant.parse("2026-10-13T12:00:00Z");
    return new OlderAdultProfilePersistenceEntity(
        "adult",
        "caregiver",
        "Rosa",
        LocalDate.of(1958, 1, 1),
        name,
        relationship,
        phone,
        now,
        now);
  }

  @Test
  void absentContactRemainsAbsent() {
    assertNull(
        OlderAdultProfilePersistenceAssembler.toDomain(profile(null, null, null))
            .emergencyContact());
  }

  @Test
  void completeContactSurvivesBothMappingDirections() {
    var adult =
        OlderAdultProfilePersistenceAssembler.toDomain(profile("Diego", "Hijo", "999111222"));
    var saved = OlderAdultProfilePersistenceAssembler.toEntity(adult);
    assertEquals("Diego", saved.getEmergencyContactName());
    assertEquals("Hijo", saved.getEmergencyContactRelationship());
    assertEquals("999111222", saved.getEmergencyContactPhone());
  }

  @Test
  void everyPartialContactCombinationIsRejectedInsteadOfDiscarded() {
    for (int mask = 1; mask < 7; mask++) {
      var stored =
          profile(
              (mask & 1) != 0 ? "Diego" : null,
              (mask & 2) != 0 ? "Hijo" : null,
              (mask & 4) != 0 ? "999111222" : null);
      assertThrows(
          IllegalArgumentException.class,
          () -> OlderAdultProfilePersistenceAssembler.toDomain(stored));
    }
  }
}
