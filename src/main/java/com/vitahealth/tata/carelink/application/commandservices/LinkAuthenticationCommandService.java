package com.vitahealth.tata.carelink.application.commandservices;

import com.vitahealth.tata.carelink.application.result.AuthenticatedCareLinkResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Coordinates consent workflow transitions and scoped credential issuance in one transaction. */
public interface LinkAuthenticationCommandService {
  /** Consumes a valid unused code and issues access restricted to its consent workflow. */
  Result<AuthenticatedCareLinkResult, ApplicationError> accept(AcceptCareLinkCommand command);

  /**
   * Issues adult access for a first approval; rejection and matching replay issue no credential.
   */
  Result<AuthenticatedCareLinkResult, ApplicationError> registerConsent(
      RegisterConsentCommand command);
}
