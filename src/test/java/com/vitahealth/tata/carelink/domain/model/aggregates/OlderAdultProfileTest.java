package com.vitahealth.tata.carelink.domain.model.aggregates;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.carelink.domain.model.valueobjects.EmergencyContact;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Verifies rejected data/contact updates cannot partially mutate adult profile state. */
class OlderAdultProfileTest {
  private static final Instant NOW = Instant.parse("2026-10-13T12:00:00Z");

  @Test
  void rejectedBasicDataUpdateRetainsItsPreviousSnapshotAndTimestamp() {
    var original = new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1));
    var replacement = new OlderAdultBasicData("Rosa Vargas", LocalDate.of(1958, 1, 1));
    var profile = OlderAdultProfile.register("caregiver", original, null, NOW);
    assertThrows(NullPointerException.class, () -> profile.updateBasicData(replacement, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> profile.updateBasicData(replacement, NOW.minusSeconds(1)));
    assertThat(profile.basicData()).isSameAs(original);
    assertThat(profile.updatedAt()).isEqualTo(NOW);
    profile.updateBasicData(replacement, NOW.plusSeconds(1));
    assertThat(profile.basicData()).isSameAs(replacement);
    assertThat(profile.createdAt()).isEqualTo(NOW);
    assertThat(profile.registeredByCaregiverId()).isEqualTo("caregiver");
  }

  @Test
  void rejectedContactUpdateDoesNotAssociateItsReplacement() {
    var original = new EmergencyContact("Diego", "Hijo", "999111222");
    var replacement = new EmergencyContact("Ana", "Hija", "999333444");
    var profile =
        OlderAdultProfile.register(
            "caregiver", new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1)), original, NOW);
    assertThrows(
        NullPointerException.class, () -> profile.associateEmergencyContact(replacement, null));
    assertThrows(
        IllegalArgumentException.class,
        () -> profile.associateEmergencyContact(replacement, NOW.minusSeconds(1)));
    assertThat(profile.emergencyContact()).isSameAs(original);
    assertThat(profile.updatedAt()).isEqualTo(NOW);
  }

  @Test
  void reconstructionRejectsImpossibleProfileChronology() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            OlderAdultProfile.rehydrate(
                "adult",
                "caregiver",
                new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1)),
                null,
                NOW,
                NOW.minusSeconds(1)));
  }
}
