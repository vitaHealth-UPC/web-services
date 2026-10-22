package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.vitahealth.tata.identitysubscription.application.result.AccountResult;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.application.result.VerifiedAccountResult;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies atomic verification responses cannot carry another account's credential. */
class VerifiedAccountResultContractTest {
  @Test
  void rejectsMismatchedOrMissingVerificationOutputs() {
    var account = new AccountResult("owner", "Rosa", "rosa@example.com", AccountStatus.ACTIVE);
    var wrongSession = new SessionResult("other", "credential", Instant.now());
    assertThrows(
        IllegalArgumentException.class, () -> new VerifiedAccountResult(account, wrongSession));
    assertThrows(NullPointerException.class, () -> new VerifiedAccountResult(account, null));
    assertThrows(NullPointerException.class, () -> new VerifiedAccountResult(null, wrongSession));
    var valid =
        new VerifiedAccountResult(account, new SessionResult("owner", "credential", Instant.now()));
    assertEquals("owner", valid.session().accountId());
  }
}
