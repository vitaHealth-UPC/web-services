package com.vitahealth.tata.carelink.application.internal.authorization;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import org.springframework.stereotype.Component;

/** Reserves caregiver-owned profile mutations without transferring registration ownership. */
@Component
public class OwnedOlderAdultProfileGuard {
  private final OlderAdultProfileRepository profiles;
  private final EnabledCaregiverAccountGuard caregivers;

  /** Injects account eligibility and profile persistence contracts. */
  public OwnedOlderAdultProfileGuard(
      OlderAdultProfileRepository profiles, EnabledCaregiverAccountGuard caregivers) {
    this.profiles = profiles;
    this.caregivers = caregivers;
  }

  /** Requires an enabled registering caregiver and a profile lock in the caller's transaction. */
  public OlderAdultProfile requireForUpdate(String caregiverId, String olderAdultId) {
    caregivers.requireEnabled(caregiverId);
    var profile =
        profiles
            .findByIdForUpdate(olderAdultId)
            .orElseThrow(OwnedOlderAdultProfileGuard::unavailable);
    if (!caregiverId.equals(profile.registeredByCaregiverId())) throw unavailable();
    return profile;
  }

  private static CareLinkApplicationException unavailable() {
    return new CareLinkApplicationException(
        CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND,
        "older adult profile is not available to this caregiver");
  }
}
