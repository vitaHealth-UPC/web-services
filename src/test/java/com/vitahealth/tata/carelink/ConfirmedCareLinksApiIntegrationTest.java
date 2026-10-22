package com.vitahealth.tata.carelink;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
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

/** Verifies consented list filtering and caregiver isolation using distinct member pairs. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class ConfirmedCareLinksApiIntegrationTest {
  @Autowired WebApplicationContext context;
  @Autowired CareLinkRepository links;
  @Autowired OlderAdultProfileRepository adults;
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  MockMvc mvc;

  @BeforeEach
  void setup() {
    mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
  }

  @Test
  void returnsOnlyConfirmedConsentedLinksForRequestedCaregiverWithoutSecrets() throws Exception {
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
    mvc =
        MockMvcBuilders.webAppContextSetup(context)
            .apply(springSecurity())
            .defaultRequest(
                get("/").header("Authorization", "Bearer " + tokens.issue(caregiver).accessToken()))
            .build();
    var now = Instant.now();
    var adult =
        adults.save(
            OlderAdultProfile.register(
                caregiver, new OlderAdultBasicData("Ana", LocalDate.of(1950, 1, 1)), null, now));
    var link =
        CareLink.createPending(
            caregiver, adult.id(), LinkingCode.issue("123456", now, Duration.ofMinutes(15)), now);
    link.accept("123456", now);
    link.registerConsent(true, now);
    link.confirm(now);
    links.save(link);
    var pendingAdult =
        adults.save(
            OlderAdultProfile.register(
                caregiver, new OlderAdultBasicData("Luis", LocalDate.of(1955, 1, 1)), null, now));
    links.save(
        CareLink.createPending(
            caregiver,
            pendingAdult.id(),
            LinkingCode.issue("654321", now, Duration.ofMinutes(15)),
            now));
    mvc.perform(get("/api/v1/care-links").param("caregiverId", caregiver))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].olderAdultId").value(adult.id()))
        .andExpect(jsonPath("$[0].olderAdultName").value("Ana"))
        .andExpect(jsonPath("$[0].linkingCode").doesNotExist());
    mvc.perform(get("/api/v1/care-links").param("caregiverId", UUID.randomUUID().toString()))
        .andExpect(status().isForbidden());
    mvc.perform(get("/api/v1/care-links").param("caregiverId", " "))
        .andExpect(status().isForbidden());
  }
}
