package com.vitahealth.tata.carelink.application.commandservices;

import com.vitahealth.tata.carelink.application.result.AdultAccessCodeResult;
import com.vitahealth.tata.carelink.application.result.AdultAccessRedemptionResult;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.CaregiverRequestResult;
import com.vitahealth.tata.carelink.application.result.ConsentDecisionResult;
import com.vitahealth.tata.carelink.application.result.ConsentWithdrawalResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.AssociateEmergencyContactCommand;
import com.vitahealth.tata.carelink.domain.model.commands.EndCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.IssueAdultAccessCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RedeemAdultAccessCodeCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.commands.UpdateOlderAdultProfileCommand;
import com.vitahealth.tata.carelink.domain.model.commands.WithdrawCareLinkConsentCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.List;

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

  /** Ends the caregiver's own relationship (removes the person from their family list). */
  Result<ConsentWithdrawalResult, ApplicationError> end(EndCareLinkCommand command);

  /** Issues a new sign-in code for the adult of a confirmed relationship. */
  Result<AdultAccessCodeResult, ApplicationError> issueAccessCode(
      IssueAdultAccessCodeCommand command);

  /** Redeems a sign-in code on the adult's device. */
  Result<AdultAccessRedemptionResult, ApplicationError> redeemAccessCode(
      RedeemAdultAccessCodeCommand command);

  /** Issues a code another caregiver uses to request caring for the same adult. */
  Result<AdultAccessCodeResult, ApplicationError> inviteCaregiver(
      String careLinkId, String caregiverId);

  /** Another caregiver joins with a code; the relationship waits for the adult. */
  Result<CareLinkResult, ApplicationError> joinAsCaregiver(String caregiverId, String code);

  /** Caregivers waiting for the adult's decision. */
  Result<List<CaregiverRequestResult>, ApplicationError> caregiverRequests(String olderAdultId);

  /** The adult accepts or rejects another caregiver. */
  Result<CareLinkResult, ApplicationError> decideCaregiverRequest(
      String olderAdultId, String careLinkId, boolean accepted);

  /** Removes the adult's account, ending every relationship. */
  Result<ConsentWithdrawalResult, ApplicationError> removeAdultAccount(String olderAdultId);

  /** Withdraws confirmed consent on behalf of the linked adult and invalidates scoped access. */
  Result<ConsentWithdrawalResult, ApplicationError> withdrawConsent(
      WithdrawCareLinkConsentCommand command);
}
