package com.vitahealth.tata.identitysubscription.application.internal.commandservices;

import com.vitahealth.tata.identitysubscription.application.commandservices.SocialSessionCommandService;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.SocialIdentityVerifier;
import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerifiedSocialIdentity;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.exceptions.DuplicateAccountEmailException;
import com.vitahealth.tata.shared.application.result.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

/** Verifies cryptographic evidence outside persistence locks; never trusts client profile input. */
@Service
public class SocialSessionCommandServiceImpl implements SocialSessionCommandService {
  private final SocialIdentityVerifier verifier;
  private final BindSocialAccountCommandHandler transactions;

  public SocialSessionCommandServiceImpl(
      SocialIdentityVerifier verifier, BindSocialAccountCommandHandler transactions) {
    this.verifier = verifier;
    this.transactions = transactions;
  }

  @Override public Result<SessionResult, ApplicationError> authenticate(String token) {
    try {
      VerifiedSocialIdentity identity = verifier.verify(token);
      return transactions.signIn(identity);
    } catch (IllegalArgumentException failure) {
      return Result.failure(new ApplicationError("INVALID_SOCIAL_CREDENTIAL", "invalid social credential"));
    } catch (DuplicateAccountEmailException | DataIntegrityViolationException failure) {
      return Result.failure(new ApplicationError("SOCIAL_LINK_CONFLICT", "social account conflict"));
    } catch (IllegalStateException failure) {
      return Result.failure(new ApplicationError("SOCIAL_PROVIDER_UNAVAILABLE", "social sign-in unavailable"));
    }
  }

  @Override public Result<Boolean, ApplicationError> link(String token, String accountId) {
    try {
      VerifiedSocialIdentity identity = verifier.verify(token);
      return transactions.link(identity, accountId);
    } catch (IllegalArgumentException failure) {
      return Result.failure(new ApplicationError("INVALID_SOCIAL_CREDENTIAL", "invalid social credential"));
    } catch (DataIntegrityViolationException failure) {
      return Result.failure(new ApplicationError("SOCIAL_LINK_CONFLICT", "social account conflict"));
    } catch (IllegalStateException failure) {
      return Result.failure(new ApplicationError("SOCIAL_PROVIDER_UNAVAILABLE", "social sign-in unavailable"));
    }
  }
}
