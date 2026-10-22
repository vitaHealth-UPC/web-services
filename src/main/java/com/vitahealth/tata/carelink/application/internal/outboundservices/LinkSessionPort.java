package com.vitahealth.tata.carelink.application.internal.outboundservices;

import com.vitahealth.tata.carelink.application.result.AuthenticatedCareLinkResult;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;

/** Exchanges accepted or confirmed relationship evidence for its scoped session credential. */
public interface LinkSessionPort {
  /** Issues onboarding only for an accepted request, or adult access for its confirmed consent. */
  AuthenticatedCareLinkResult establish(CareLinkResult link);
}
