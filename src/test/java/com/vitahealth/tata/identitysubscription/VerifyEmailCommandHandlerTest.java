package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.vitahealth.tata.identitysubscription.application.internal.commandservices.VerifyEmailCommandHandler;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.vitahealth.tata.identitysubscription.domain.model.events.AccountEnabled;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import java.time.*;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

/** Checks the extracted verification policy, including no mutation on a rejected challenge. */
class VerifyEmailCommandHandlerTest {
  @Test
  void malformedCodeDoesNotAcquireLockOrInvokeHasher() {
    for (String code : new String[] {null, "", "12345", "1234567", "１２３４５６", "12345x"}) {
      assertThat(handler.handle(new VerifyEmailCommand("owner@example.com", code)).toOptional())
          .isEmpty();
    }
    verifyNoInteractions(accounts, hasher, events);
  }

  private final Instant now = Instant.parse("2026-10-11T12:00:00Z");
  private final AccountRepository accounts = mock(AccountRepository.class);
  private final PasswordHasher hasher = mock(PasswordHasher.class);
  private final ApplicationEventPublisher events = mock(ApplicationEventPublisher.class);
  private final VerifyEmailCommandHandler handler =
      new VerifyEmailCommandHandler(accounts, hasher, events, Clock.fixed(now, ZoneOffset.UTC));

  @Test
  void validChallengeEnablesLockedAccountAndPublishesItsEvent() {
    var account = account();
    when(accounts.findByEmailForUpdate("owner@example.com")).thenReturn(Optional.of(account));
    when(hasher.matches("123456", "challenge-hash")).thenReturn(true);
    when(accounts.save(account)).thenReturn(account);

    var result = handler.handle(new VerifyEmailCommand("owner@example.com", "123456"));

    assertThat(result.toOptional().orElseThrow().status()).isEqualTo(AccountStatus.ACTIVE);
    verify(events).publishEvent(any(AccountEnabled.class));
    verify(accounts, never()).findByEmail(any());
  }

  @Test
  void invalidChallengeDoesNotSaveOrPublish() {
    var account = account();
    when(accounts.findByEmailForUpdate("owner@example.com")).thenReturn(Optional.of(account));
    assertThat(handler.handle(new VerifyEmailCommand("owner@example.com", "wrong")).toOptional())
        .isEmpty();
    assertThat(account.status()).isEqualTo(AccountStatus.PENDING_VERIFICATION);
    verify(accounts, never()).save(any());
    verifyNoInteractions(events);
  }

  private Account account() {
    return Account.register(
        "Owner",
        new EmailAddress("owner@example.com"),
        "password-hash",
        "challenge-hash",
        now,
        Duration.ofMinutes(15));
  }
}
