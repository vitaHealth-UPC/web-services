package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.commands.AssociateEmergencyContactCommand;
import com.vitahealth.tata.carelink.domain.model.commands.UpdateOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * Verifies persisted profile changes, caller ownership and atomic rejection through real sessions.
 */
@SpringBootTest
@ActiveProfiles("test")
class OlderAdultProfileChangesApiIntegrationTest {
  @Autowired private WebApplicationContext context;
  @Autowired private OlderAdultProfileRepository profiles;
  @Autowired private CareLinkCommandService commands;

  @Test
  void foreignCaregiverCommandsReturnFailureWithoutChangingProfileOrContact() {
    var owner = CareLinkHttpFixture.awaiting(context);
    var foreign = CareLinkHttpFixture.awaiting(context);
    var before = profiles.findById(owner.olderAdultId()).orElseThrow();
    var update =
        commands.updateOlderAdultProfile(
            new UpdateOlderAdultProfileCommand(
                foreign.caregiverId(), owner.olderAdultId(), "Changed", LocalDate.of(1958, 1, 1)));
    var contact =
        commands.associateEmergencyContact(
            new AssociateEmergencyContactCommand(
                foreign.caregiverId(), owner.olderAdultId(), "Changed", "Family", "999111222"));
    for (var result : List.of(update, contact)) {
      assertThat(result.isFailure()).isTrue();
      var failure = (Result.Failure<OlderAdultProfileResult, ApplicationError>) result;
      assertThat(failure.error().code()).isEqualTo("OLDER_ADULT_NOT_FOUND");
    }
    var after = profiles.findById(owner.olderAdultId()).orElseThrow();
    assertThat(after.basicData()).isEqualTo(before.basicData());
    assertThat(after.emergencyContact()).isEqualTo(before.emergencyContact());
    assertThat(after.updatedAt()).isEqualTo(before.updatedAt());
  }

  @Test
  void registeringCaregiverCanEditBeforeConsentWithoutChangingOwnershipOrCreation()
      throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var before = profiles.findById(fixture.olderAdultId()).orElseThrow();
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    mvc.perform(
            put("/api/v1/older-adults/{id}", fixture.olderAdultId())
                .header("Authorization", "Bearer " + fixture.caregiverToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Rosa Vargas\",\"birthDate\":\"1958-02-02\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.fullName").value("Rosa Vargas"));
    var after = profiles.findById(fixture.olderAdultId()).orElseThrow();
    assertThat(after.basicData().fullName()).isEqualTo("Rosa Vargas");
    assertThat(after.registeredByCaregiverId()).isEqualTo(before.registeredByCaregiverId());
    assertThat(after.createdAt()).isEqualTo(before.createdAt());
  }

  @Test
  void contactAssociationStoresAllFieldsWithoutReplacingBasicData() throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    mvc.perform(
            put("/api/v1/older-adults/{id}/emergency-contact", fixture.olderAdultId())
                .header("Authorization", "Bearer " + fixture.caregiverToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"fullName\":\"Diego\",\"relationship\":\"Hijo\",\"phone\":\"999111222\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.emergencyContactName").value("Diego"));
    var saved = profiles.findById(fixture.olderAdultId()).orElseThrow();
    assertThat(saved.basicData().fullName()).isEqualTo("Rosa");
    assertThat(saved.emergencyContact().phone()).isEqualTo("999111222");
  }

  @Test
  void anotherCaregiverCannotModifyProfileOrContact() throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var other = CareLinkHttpFixture.awaiting(context);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    for (String suffix : new String[] {"", "/emergency-contact"}) {
      mvc.perform(
              put("/api/v1/older-adults/" + fixture.olderAdultId() + suffix)
                  .header("Authorization", "Bearer " + other.caregiverToken())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content("{}"))
          .andExpect(status().isForbidden());
    }
    assertThat(profiles.findById(fixture.olderAdultId()).orElseThrow().basicData().fullName())
        .isEqualTo("Rosa");
  }

  @Test
  void invalidBirthDateAndPartialContactLeaveStoredProfileUnchanged() throws Exception {
    var fixture = CareLinkHttpFixture.awaiting(context);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    mvc.perform(
            put("/api/v1/older-adults/{id}", fixture.olderAdultId())
                .header("Authorization", "Bearer " + fixture.caregiverToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Changed\",\"birthDate\":\"2999-01-01\"}"))
        .andExpect(status().isBadRequest());
    mvc.perform(
            put("/api/v1/older-adults/{id}/emergency-contact", fixture.olderAdultId())
                .header("Authorization", "Bearer " + fixture.caregiverToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Diego\"}"))
        .andExpect(status().isBadRequest());
    var saved = profiles.findById(fixture.olderAdultId()).orElseThrow();
    assertThat(saved.basicData().fullName()).isEqualTo("Rosa");
    assertThat(saved.emergencyContact()).isNull();
  }

  @Test
  void adultSessionCannotActAsRegisteringCaregiver() throws Exception {
    var fixture = CareLinkHttpFixture.approved(context);
    var mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    mvc.perform(
            put("/api/v1/older-adults/{id}", fixture.olderAdultId())
                .header("Authorization", "Bearer " + fixture.adultToken())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"fullName\":\"Changed\",\"birthDate\":\"1958-01-01\"}"))
        .andExpect(status().isForbidden());
    assertThat(profiles.findById(fixture.olderAdultId()).orElseThrow().basicData().fullName())
        .isEqualTo("Rosa");
  }
}
