package com.vitahealth.tata.intakeexecution;

import static org.hamcrest.Matchers.contains;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.vitahealth.tata.intakeexecution.domain.repositories.IntakeRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** Verifies agenda authorization, interval semantics and documented public representations. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class IntakeAgendaIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired IntakeRepository repository;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  @Autowired CareLinkCommandService links;
  String owner;
  MockMvc mvc;
  final Instant from = Instant.parse("2026-10-05T05:00:00Z");

  @BeforeEach
  void setUp() {
    String caregiver = UUID.randomUUID().toString();
    accounts.save(
        Account.rehydrate(
            caregiver,
            "Caregiver",
            new EmailAddress(caregiver + "@example.com"),
            "hash",
            AccountStatus.ACTIVE,
            null,
            null));
    var profile =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiver, "Rosa", LocalDate.of(1950, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    owner = profile.id();
    var generated =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiver, owner))
            .toOptional()
            .orElseThrow();
    var accepted =
        links
            .accept(new AcceptCareLinkCommand(caregiver, generated.linkingCode()))
            .toOptional()
            .orElseThrow();
    links
        .registerConsent(new RegisterConsentCommand(accepted.id(), true))
        .toOptional()
        .orElseThrow();
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/")
                    .header(
                        "Authorization", "Bearer " + tokens.issueOlderAdult(owner).accessToken()))
            .build();
  }

  private Intake intake(String owner, Instant scheduled) {
    return Intake.createScheduled(
        UUID.randomUUID().toString(),
        UUID.randomUUID().toString(),
        owner,
        new MedicationSnapshot("Losartan", "1 tablet", "With water"),
        scheduled,
        from);
  }

  @Test
  void documentsQueryResourcesAndDistinctEmptyResponses() throws Exception {
    mvc.perform(get("/v3/api-docs"))
        .andExpect(status().isOk())
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/older-adults/{olderAdultId}/intakes/agenda']['get']['responses']['200']['content']['application/json']['schema']['type']")
                .value("array"))
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/older-adults/{olderAdultId}/intakes/agenda']['get']['responses']['200']['content']['application/json']['schema']['items']['$ref']")
                .value("#/components/schemas/IntakeResource"))
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/older-adults/{olderAdultId}/intakes/next']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                .value("#/components/schemas/IntakeResource"))
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/older-adults/{olderAdultId}/intakes/next']['get']['responses']['204']['content']")
                .doesNotExist())
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/intakes/{intakeId}']['get']['responses']['200']['content']['application/json']['schema']['$ref']")
                .value("#/components/schemas/IntakeResource"))
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/intakes/{intakeId}/confirmation']['post']['responses']['200']['content']['application/json']['schema']['$ref']")
                .value("#/components/schemas/IntakeResource"))
        .andExpect(
            jsonPath(
                    "$['paths']['/api/v1/intakes/{intakeId}/voice-confirmation']['post']['responses']['200']['content']['application/json']['schema']['$ref']")
                .value("#/components/schemas/VoiceConfirmationResource"))
        .andExpect(
            jsonPath(
                    "$['components']['schemas']['ConfirmIntakeResource']['properties']['channel']['enum']")
                .value(contains("TOUCH")));
  }

  @Test
  void agendaIsOrderedScopedAndIncludesResolvedHistoryWithExclusiveEnd() throws Exception {
    var first = intake(owner, from);
    first.confirm(from, ConfirmationChannel.TOUCH);
    repository.saveAll(
        List.of(
            intake(owner, from.plusSeconds(3600)),
            first,
            intake(owner, from.plusSeconds(86400)),
            intake(UUID.randomUUID().toString(), from),
            intake(owner, from.minusSeconds(1))));
    mvc.perform(
            get("/api/v1/older-adults/{id}/intakes/agenda", owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(86400).toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].id").value(first.id()))
        .andExpect(jsonPath("$[0].status").value("CONFIRMED"))
        .andExpect(jsonPath("$[0].confirmedAt").exists())
        .andExpect(jsonPath("$[1].status").value("PENDING"));
  }

  @Test
  void emptyAgendaReturnsAnEmptyArray() throws Exception {
    mvc.perform(
            get("/api/v1/older-adults/{id}/intakes/agenda", owner)
                .param("from", from.toString())
                .param("to", from.plusSeconds(86400).toString()))
        .andExpect(status().isOk())
        .andExpect(content().json("[]"));
  }

  @Test
  void rejectsReversedUnboundedAndMalformedRanges() throws Exception {
    var path = "/api/v1/older-adults/" + owner + "/intakes/agenda";
    mvc.perform(get(path).param("from", from.toString()).param("to", from.toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(
            get(path)
                .param("from", from.toString())
                .param("to", from.plusSeconds(9 * 86400).toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(get(path).param("from", "invalid").param("to", from.toString()))
        .andExpect(status().isBadRequest());
    mvc.perform(get(path)).andExpect(status().isBadRequest());
  }

  @Test
  void anotherAdultsAgendaIsDenied() throws Exception {
    mvc.perform(
            get("/api/v1/older-adults/{id}/intakes/agenda", UUID.randomUUID())
                .param("from", from.toString())
                .param("to", from.plusSeconds(86400).toString()))
        .andExpect(status().isForbidden());
  }
}
