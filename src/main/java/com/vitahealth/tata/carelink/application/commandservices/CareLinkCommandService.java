package com.vitahealth.tata.carelink.application.commandservices;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.ConsentDecisionResult;
import com.vitahealth.tata.carelink.application.result.ConsentWithdrawalResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.AssociateEmergencyContactCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.commands.UpdateOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;

/** Public command boundary for adult profiles, linking evidence and explicit adult consent. */
public interface CareLinkCommandService {
  /** Changes profile data owned by the enabled registering caregiver. */
  Result<OlderAdultProfileResult, ApplicationError> associateEmergencyContact(
      AssociateEmergencyContactCommand command);

  /** Changes profile data owned by the enabled registering caregiver. */
  Result<OlderAdultProfileResult, ApplicationError> updateOlderAdultProfile(
      UpdateOlderAdultProfileCommand command);

  /** Registers an adult profile on behalf of an enabled caregiver account. */
  Result<OlderAdultProfileResult, ApplicationError> registerOlderAdult(
      RegisterOlderAdultProfileCommand command);

  /** Issues or returns a live pending code without duplicating the caregiver/profile pair. */
  Result<CareLinkResult, ApplicationError> generateLinkingCode(GenerateLinkingCodeCommand command);

  /** Consumes the caregiver's valid linking code and starts consent onboarding. */
  Result<CareLinkResult, ApplicationError> accept(AcceptCareLinkCommand command);

  /** Reports first decision versus matching replay for credential-issuing workflows. */
  Result<ConsentDecisionResult, ApplicationError> registerConsentDecision(
      RegisterConsentCommand command);

  /** Records the adult decision and confirms or rejects the relationship. */
  Result<CareLinkResult, ApplicationError> registerConsent(RegisterConsentCommand command);

  /** Withdraws confirmed consent on behalf of the linked adult and invalidates scoped access. */
  Result<ConsentWithdrawalResult, ApplicationError> withdrawConsent(
      WithdrawCareLinkConsentCommand command);
}
