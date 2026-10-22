package com.vitahealth.tata.carelink.domain.model.valueobjects;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Verifies persisted text bounds and birth-date validation against supplied domain time. */
class ProfileDataContractTest {
  @Test
  void diagnosticOutputOmitsPersonalFieldsWithoutChangingValueAccessors() {
    var data = new OlderAdultBasicData("Rosa Vargas", LocalDate.of(1958, 1, 1));
    var contact = new EmergencyContact("Diego", "Hijo", "999111222");
    assertThat(data.toString()).doesNotContain("Rosa Vargas", "1958-01-01");
    assertThat(contact.toString()).doesNotContain("Diego", "999111222");
    assertThat(data.fullName()).isEqualTo("Rosa Vargas");
    assertThat(contact.phone()).isEqualTo("999111222");
  }

  @Test
  void namesAndContactFieldsRespectTheirActualStorageBounds() {
    var birthday = LocalDate.of(1958, 1, 1);
    assertThat(new OlderAdultBasicData("\u2003Rosa\u2003", birthday).fullName()).isEqualTo("Rosa");
    assertThat(new OlderAdultBasicData("x".repeat(160), birthday).fullName()).hasSize(160);
    assertThrows(
        IllegalArgumentException.class, () -> new OlderAdultBasicData("x".repeat(161), birthday));
    new EmergencyContact("x".repeat(160), "x".repeat(80), "1".repeat(40));
    assertThrows(
        IllegalArgumentException.class, () -> new EmergencyContact("x".repeat(161), "Hijo", "123"));
    assertThrows(
        IllegalArgumentException.class, () -> new EmergencyContact("Diego", "x".repeat(81), "123"));
    assertThrows(
        IllegalArgumentException.class,
        () -> new EmergencyContact("Diego", "Hijo", "1".repeat(41)));
  }

  @Test
  void futureBirthDateIsRejectedAtRegistrationAndUpdateWithoutMutatingTheProfile() {
    var now = Instant.parse("2020-01-01T12:00:00Z");
    var future = new OlderAdultBasicData("Rosa", LocalDate.of(2020, 1, 2));
    assertThrows(
        IllegalArgumentException.class,
        () -> OlderAdultProfile.register("caregiver", future, null, now));
    var original = new OlderAdultBasicData("Rosa", LocalDate.of(1958, 1, 1));
    var profile = OlderAdultProfile.register("caregiver", original, null, now);
    assertThrows(IllegalArgumentException.class, () -> profile.updateBasicData(future, now));
    assertThat(profile.basicData()).isSameAs(original);
    assertThat(profile.updatedAt()).isEqualTo(now);
    assertThat(
            OlderAdultProfile.register(
                    "caregiver",
                    new OlderAdultBasicData("Rosa", LocalDate.of(2020, 1, 1)),
                    null,
                    now)
                .basicData()
                .birthDate())
        .isEqualTo(LocalDate.of(2020, 1, 1));
  }
}
