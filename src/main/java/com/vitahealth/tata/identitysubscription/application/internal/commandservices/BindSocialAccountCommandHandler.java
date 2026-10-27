package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerifiedSocialIdentity;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.hashing.PasswordHasher;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.tokens.SessionTokenService;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.model.aggregates.Account;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.EmailAddress;
import com.vitahealth.tata.identitysubscription.domain.repositories.AccountRepository;
import com.vitahealth.tata.identitysubscription.domain.repositories.SocialAccountLinkRepository;
import com.vitahealth.tata.shared.application.result.*;
import java.security.SecureRandom;
import java.time.Clock;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** All account creation and unique external identity binding commit before session issuance. */
@Service
@Transactional
public class BindSocialAccountCommandHandler {
  private final AccountRepository accounts;
  private final SocialAccountLinkRepository links;
  private final PasswordHasher hasher;
  private final SessionTokenService sessions;
  private final Clock clock;
  private final SecureRandom random = new SecureRandom();

  public BindSocialAccountCommandHandler(AccountRepository accounts,
      SocialAccountLinkRepository links, PasswordHasher hasher, SessionTokenService sessions,
      Clock clock) {
    this.accounts = accounts;
    this.links = links;
    this.hasher = hasher;
    this.sessions = sessions;
    this.clock = clock;
  }

  public Result<SessionResult, ApplicationError> signIn(VerifiedSocialIdentity verified) {
    var existing = links.find(verified.issuer(), verified.subject());
    if (existing.isPresent()) {
      var account = accounts.findByIdForUpdate(existing.get().accountId());
      if (account.isEmpty() || !account.get().canAuthenticate())
        return Result.failure(new ApplicationError("ACCOUNT_NOT_ACTIVE", "account is not active"));
      return Result.success(sessions.issue(account.get().id()));
    }
    // A provider email, even if asserted as verified, never authorizes entry to an EXISTING account.
    if (!verified.emailVerified() || verified.email() == null)
      return Result.failure(new ApplicationError("SOCIAL_EMAIL_REQUIRED", "verified email required"));
    var email = new EmailAddress(verified.email());
    if (accounts.findByEmailForUpdate(email.value()).isPresent())
      return Result.failure(new ApplicationError("SOCIAL_LINK_REQUIRED", "explicit account linking required"));
    var raw = new byte[32];
    random.nextBytes(raw);
    var name = verified.displayName() == null || verified.displayName().isBlank()
        ? email.value().substring(0, email.value().indexOf('@')) : verified.displayName().trim();
    if (name.length() > 120) name = name.substring(0, 120);
    var account = Account.registerSocial(name, email,
        hasher.hash(HexFormat.of().formatHex(raw)), clock.instant());
    accounts.save(account);
    links.bind(new SocialAccountLinkRepository.Link(
        verified.issuer(), verified.subject(), account.id(), verified.provider().name()));
    return Result.success(sessions.issue(account.id()));
  }

  public Result<Boolean, ApplicationError> link(VerifiedSocialIdentity verified, String accountId) {
    var account = accounts.findByIdForUpdate(accountId);
    if (account.isEmpty() || !account.get().canAuthenticate())
      return Result.failure(new ApplicationError("ACCOUNT_NOT_ACTIVE", "account is not active"));
    var existing = links.find(verified.issuer(), verified.subject());
    if (existing.isPresent()) {
      if (existing.get().accountId().equals(accountId)) return Result.success(true);
      return Result.failure(new ApplicationError("SOCIAL_LINK_CONFLICT", "identity already linked"));
    }
    links.bind(new SocialAccountLinkRepository.Link(
        verified.issuer(), verified.subject(), accountId, verified.provider().name()));
    return Result.success(true);
  }
}
