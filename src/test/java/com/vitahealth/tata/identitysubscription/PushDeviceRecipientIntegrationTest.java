package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.queryservices.SessionRegistrationQueryService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.repositories.PushDeviceRegistrationRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.AccountSessionPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.AccountSessionJpaRepository;
import com.vitahealth.tata.identitysubscription.interfaces.acl.PushDeviceContextFacade;
import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.NotificationRecipientPort;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Exercises bounded recipient SQL with real revocable sessions and account authorization. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class PushDeviceRecipientIntegrationTest {
  @Autowired AccountRepository accounts;
  @Autowired SessionTokenService tokens;
  @Autowired SessionRegistrationQueryService sessions;
  @Autowired AccountSessionJpaRepository sessionRows;
  @Autowired PushDeviceRegistrationRepository devices;
  @Autowired PushDeviceContextFacade recipients;
  @Autowired Clock clock;
  @Autowired CareLinkCommandService links;
  @Autowired NotificationRecipientPort notificationRecipients;
  @Autowired EntityManager entities;
  @Autowired JdbcTemplate jdbc;

  private Account account() {
    var account =
        Account.register(
            "Push owner",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "hash",
            "challenge",
            clock.instant(),
            Duration.ofMinutes(15));
    account.completeVerification();
    return accounts.save(account);
  }

  private PushDeviceRegistration register(String sessionId) {
    return devices.save(
        PushDeviceRegistration.register(
            UUID.randomUUID().toString(),
            sessionId,
            UUID.randomUUID().toString(),
            clock.instant()));
  }

  @Test
  void keysetReadsAll105DevicesWithoutDuplicatesAndExcludesAnotherOwnerAndRole() {
    var owner = account();
    var other = account();
    var raw = tokens.issue(owner.id()).accessToken();
    var session = sessions.authenticateRegistration(raw).orElseThrow().sessionId();
    for (int i = 0; i < 105; i++) register(session);
    register(
        sessions
            .authenticateRegistration(tokens.issue(other.id()).accessToken())
            .orElseThrow()
            .sessionId());
    entities.flush();
    entities.clear();
    var first = recipients.findCaregiverDevices(owner.id(), "", 100);
    assertThat(first.devices()).hasSize(100);
    assertThat(first.nextCursor()).isNotNull();
    var last = recipients.findCaregiverDevices(owner.id(), first.nextCursor(), 100);
    assertThat(last.devices()).hasSize(5);
    assertThat(last.nextCursor()).isNull();
    var ids = new ArrayList<String>();
    first.devices().forEach(device -> ids.add(device.installationId()));
    last.devices().forEach(device -> ids.add(device.installationId()));
    assertThat(ids).doesNotHaveDuplicates().isSorted();
    assertThat(recipients.findOlderAdultDevices(owner.id(), "", 100).devices()).isEmpty();
  }

  @Test
  void revocationAndExpiryRemoveCandidateAddressesFromSql() {
    var owner = account();
    var raw = tokens.issue(owner.id()).accessToken();
    var session = sessions.authenticateRegistration(raw).orElseThrow().sessionId();
    register(session);
    assertThat(recipients.findCaregiverDevices(owner.id(), "", 100).devices()).hasSize(1);
    tokens.revoke(raw);
    entities.flush();
    entities.clear();
    assertThat(recipients.findCaregiverDevices(owner.id(), "", 100).devices()).isEmpty();
    var expired =
        sessionRows.save(
            new AccountSessionPersistenceEntity(
                owner.id(),
                UUID.randomUUID().toString().replace("-", ""),
                clock.instant().minusSeconds(1),
                "CAREGIVER",
                null));
    register(expired.getId());
    entities.flush();
    entities.clear();
    assertThat(recipients.findCaregiverDevices(owner.id(), "", 100).devices()).isEmpty();
  }

  @Test
  void disabledAccountIsFilteredWithoutLosingTheNextPageCursor() {
    var owner = account();
    var raw = tokens.issue(owner.id()).accessToken();
    var session = sessions.authenticateRegistration(raw).orElseThrow().sessionId();
    var device = register(session);
    entities.flush();
    jdbc.update("update identity_accounts set status = 'DISABLED' where id = ?", owner.id());
    entities.clear();
    var first = recipients.findCaregiverDevices(owner.id(), "", 1);
    assertThat(first.devices()).isEmpty();
    assertThat(first.nextCursor()).isEqualTo(device.installationId());
    var last = recipients.findCaregiverDevices(owner.id(), first.nextCursor(), 1);
    assertThat(last.devices()).isEmpty();
    assertThat(last.nextCursor()).isNull();
  }

  @Test
  void adultAddressRequiresConfirmedConsentAndDisappearsOnWithdrawal() {
    var caregiver = account();
    var adult =
        links
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    caregiver.id(), "Ana", LocalDate.of(1950, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var link =
        links
            .generateLinkingCode(new GenerateLinkingCodeCommand(caregiver.id(), adult.id()))
            .toOptional()
            .orElseThrow();
    assertThat(
            links.accept(new AcceptCareLinkCommand(caregiver.id(), link.linkingCode())).isSuccess())
        .isTrue();
    assertThat(links.registerConsent(new RegisterConsentCommand(link.id(), true)).isSuccess())
        .isTrue();
    var caregiverSession =
        sessions
            .authenticateRegistration(tokens.issue(caregiver.id()).accessToken())
            .orElseThrow()
            .sessionId();
    register(caregiverSession);
    var raw = tokens.issueOlderAdult(adult.id(), link.id()).accessToken();
    var session = sessions.authenticateRegistration(raw).orElseThrow().sessionId();
    register(session);
    entities.flush();
    entities.clear();
    assertThat(recipients.findOlderAdultDevices(adult.id(), "", 100).devices()).hasSize(1);
    assertThat(notificationRecipients.findCaregivers(adult.id(), "", 100).caregiverIds())
        .containsExactly(caregiver.id());
    assertThat(
            notificationRecipients
                .findCaregiverDevices(adult.id(), caregiver.id(), "", 100)
                .devices())
        .hasSize(1);
    assertThat(notificationRecipients.findOlderAdultDevices(adult.id(), "", 100).devices())
        .hasSize(1);
    assertThat(recipients.findCaregiverDevices(adult.id(), "", 100).devices()).isEmpty();
    assertThat(
            links
                .withdrawConsent(new WithdrawCareLinkConsentCommand(link.id(), adult.id()))
                .isSuccess())
        .isTrue();
    entities.flush();
    entities.clear();
    assertThat(recipients.findOlderAdultDevices(adult.id(), "", 100).devices()).isEmpty();
    assertThat(recipients.findCaregiverDevices(caregiver.id(), "", 100).devices()).hasSize(1);
    assertThat(notificationRecipients.findCaregivers(adult.id(), "", 100).caregiverIds()).isEmpty();
    assertThat(
            notificationRecipients
                .findCaregiverDevices(adult.id(), caregiver.id(), "", 100)
                .devices())
        .isEmpty();
    assertThat(notificationRecipients.findOlderAdultDevices(adult.id(), "", 100).devices())
        .isEmpty();
  }
}
