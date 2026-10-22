package com.vitahealth.tata.treatmentmanagement.infrastructure.acl;

import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.treatmentmanagement.application.internal.outboundservices.CareLinkVerificationPort;
import org.springframework.stereotype.Component;

/** Delegates current relationship authorization through Care Link public ACL contracts. */
@Component
public class CareLinkVerificationAdapter implements CareLinkVerificationPort {
  private final CareLinkContextFacade careLinks;

  /** Injects the external context facade without importing its repositories. */
  public CareLinkVerificationAdapter(CareLinkContextFacade careLinks) {
    this.careLinks = careLinks;
  }

  /** Preserves denied access and lets technical read failures reach the caller. */
  @Override
  public boolean isAuthorized(String caregiverId, String olderAdultId) {
    return careLinks.isAuthorized(caregiverId, olderAdultId);
  }
}
