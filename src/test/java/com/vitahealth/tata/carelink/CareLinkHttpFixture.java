package com.vitahealth.tata.carelink;

import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
import com.vitahealth.tata.carelink.application.commandservices.LinkAuthenticationCommandService;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.web.context.WebApplicationContext;

/**
 * Creates persisted ownership and credentials using the actual onboarding application contracts.
 */
public record CareLinkHttpFixture(
    String linkId,
    String caregiverId,
    String olderAdultId,
    String caregiverToken,
    String setupToken,
    String adultToken) {
  /** Creates an adult session with explicit approved care-link consent. */
  public static CareLinkHttpFixture approved(WebApplicationContext context) {
    return create(context, true);
  }

  /** Creates credentials restricted to the still-pending consent flow. */
  public static CareLinkHttpFixture awaiting(WebApplicationContext context) {
    return create(context, false);
  }

  private static CareLinkHttpFixture create(WebApplicationContext context, boolean approved) {
    var account =
        Account.register(
            "Caregiver",
            new EmailAddress(UUID.randomUUID() + "@example.com"),
            "encoded-password",
            "encoded-challenge",
            Instant.now(),
            Duration.ofMinutes(15));
    account.completeVerification();
    account = context.getBean(AccountRepository.class).save(account);
    var commands = context.getBean(CareLinkCommandService.class);
    var authentication = context.getBean(LinkAuthenticationCommandService.class);
    var adult =
        commands
            .registerOlderAdult(
                new RegisterOlderAdultProfileCommand(
                    account.id(), "Rosa", LocalDate.of(1958, 1, 1), null, null, null))
            .toOptional()
            .orElseThrow();
    var pending =
        commands
            .generateLinkingCode(new GenerateLinkingCodeCommand(account.id(), adult.id()))
            .toOptional()
            .orElseThrow();
    var setup =
        authentication
            .accept(new AcceptCareLinkCommand(account.id(), pending.linkingCode()))
            .toOptional()
            .orElseThrow();
    var token =
        approved
            ? authentication
                .registerConsent(new RegisterConsentCommand(pending.id(), true))
                .toOptional()
                .orElseThrow()
                .accessToken()
            : null;
    return new CareLinkHttpFixture(
        pending.id(),
        account.id(),
        adult.id(),
        context.getBean(SessionTokenService.class).issue(account.id()).accessToken(),
        setup.accessToken(),
        token);
  }

  @Override
  public String toString() {
    return "CareLinkHttpFixture[credentials=REDACTED]";
  }
}
