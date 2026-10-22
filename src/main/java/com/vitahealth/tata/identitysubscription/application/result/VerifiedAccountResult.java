package com.vitahealth.tata.identitysubscription.application.result;

import java.util.Objects;

/** Output of atomic account verification and session issuance for the verified owner. */
public record VerifiedAccountResult(AccountResult account, SessionResult session) {
  /** Requires a complete verification outcome whose credential belongs to the verified account. */
  public VerifiedAccountResult {
    Objects.requireNonNull(account, "account");
    Objects.requireNonNull(session, "session");
    if (account.id() == null || !account.id().equals(session.accountId())) {
      throw new IllegalArgumentException("Verified account and session subjects must agree");
    }
  }
}
