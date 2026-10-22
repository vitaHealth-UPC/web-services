package com.vitahealth.tata.carelink.interfaces.acl;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.CaregiverRecipientPageResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import java.util.List;
import java.util.Optional;

/** Cross-context reads of relationship authorization and adult profile evidence. */
public interface CareLinkContextFacade {
  /** Returns empty for a missing relationship without absorbing technical failures. */
  Optional<CareLinkResult> findById(String careLinkId);

  /** Requires a relationship by its identifier. */
  CareLinkResult getById(String careLinkId);

  /** Requires an adult profile by its identifier. */
  OlderAdultProfileResult getOlderAdult(String olderAdultId);

  /** Checks whether the caregiver currently holds consented access to the adult. */
  boolean isAuthorized(String caregiverId, String olderAdultId);

  /** Lists confirmed relationships belonging to the caregiver. */
  List<CareLinkResult> getConfirmedByCaregiver(String caregiverId);

  /** Returns empty for an absent profile; infrastructure failures remain visible to callers. */
  Optional<OlderAdultProfileResult> findOlderAdult(String olderAdultId);

  /** Scans credential-free caregiver recipients currently authorized for the requested adult. */
  CaregiverRecipientPageResult findAuthorizedCaregivers(
      String olderAdultId, String afterId, int limit);
}
