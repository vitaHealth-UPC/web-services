package com.vitahealth.tata.carelink.application.internal.authorization;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.outboundservices.AccountStatusPort;
import org.springframework.stereotype.Component;

/**
 * Checks caregiver eligibility through Identity's public account boundary for Care Link commands.
 */
@Component
public final class EnabledCaregiverAccountGuard {
  private final AccountStatusPort accounts;

  /** Injects live account eligibility without accessing another context's persistence. */
  public EnabledCaregiverAccountGuard(AccountStatusPort accounts) {
    this.accounts = accounts;
  }

  /** Rejects missing or disabled accounts while letting technical lookup failures propagate. */
  public void requireEnabled(String caregiverId) {
    if (caregiverId == null || caregiverId.isBlank() || !accounts.isEnabled(caregiverId)) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.ACCOUNT_NOT_ENABLED,
          "caregiver account is not enabled");
    }
  }
}
