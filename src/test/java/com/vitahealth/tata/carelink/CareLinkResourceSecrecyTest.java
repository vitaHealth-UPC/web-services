package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.interfaces.rest.resources.AcceptCareLinkResource;
import com.vitahealth.tata.carelink.interfaces.rest.resources.CareLinkResource;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Checks record diagnostics hide credentials while accessors preserve their existing contracts. */
class CareLinkResourceSecrecyTest {
  @Test
  void credentialAccessorsRemainAvailableWithoutDiagnosticExposure() {
    var now = Instant.parse("2026-10-13T12:00:00Z");
    var response =
        new CareLinkResource(
            "link",
            "caregiver",
            "adult",
            CareLinkStatus.AWAITING_CONSENT,
            "TATA-4821",
            now.plusSeconds(900),
            now,
            false,
            null,
            null,
            "private-token",
            now.plusSeconds(900));
    assertThat(response.toString()).doesNotContain("TATA-4821", "private-token");
    assertThat(response.linkingCode()).isEqualTo("TATA-4821");
    assertThat(response.accessToken()).isEqualTo("private-token");
    var request = new AcceptCareLinkResource("caregiver", "TATA-4821");
    assertThat(request.toString()).doesNotContain("TATA-4821");
    assertThat(request.code()).isEqualTo("TATA-4821");
    var command = new AcceptCareLinkCommand("caregiver", "TATA-4821");
    assertThat(command.toString()).doesNotContain("TATA-4821");
    assertThat(command.code()).isEqualTo("TATA-4821");
    var profile =
        new RegisterOlderAdultProfileCommand(
            "caregiver", "Rosa Vargas", LocalDate.of(1958, 1, 1), "Diego", "Hijo", "999111222");
    assertThat(profile.toString()).doesNotContain("Rosa Vargas", "Diego", "999111222");
    assertThat(profile.fullName()).isEqualTo("Rosa Vargas");
    var profileResult =
        new OlderAdultProfileResult(
            "adult",
            "caregiver",
            "Rosa Vargas",
            LocalDate.of(1958, 1, 1),
            "Diego",
            "Hijo",
            "999111222",
            now);
    assertThat(profileResult.toString()).doesNotContain("Rosa Vargas", "Diego", "999111222");
    var linkResult =
        new CareLinkResult(
            "link",
            "caregiver",
            "adult",
            CareLinkStatus.PENDING,
            "TATA-4821",
            now.plusSeconds(900),
            null,
            false,
            null,
            null);
    assertThat(linkResult.toString()).doesNotContain("TATA-4821");
    assertThat(linkResult.linkingCode()).isEqualTo("TATA-4821");
  }
}
