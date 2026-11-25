package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
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
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Routes public Care Link commands to handlers that own their individual transactions. */
@Service
public class CareLinkCommandServiceImpl implements CareLinkCommandService {
  private final UpdateOlderAdultProfileCommandHandler updateOlderAdultProfileHandler;
  private final AssociateEmergencyContactCommandHandler associateEmergencyContactHandler;
  private final RegisterOlderAdultProfileCommandHandler profiles;
  private final GenerateLinkingCodeCommandHandler codes;
  private final AcceptCareLinkCommandHandler acceptances;
  private final RegisterConsentCommandHandler consents;
  private final WithdrawCareLinkConsentCommandHandler withdrawals;
  private final EndCareLinkCommandHandler ends;
  private final AdultAccessCodeCommandHandler accessCodes;
  private final CoCaregiverCommandHandler coCaregivers;

  /**
   * Injects concrete command handlers without duplicating domain or persistence responsibilities.
   */
  public CareLinkCommandServiceImpl(
      UpdateOlderAdultProfileCommandHandler updateOlderAdultProfileHandler,
      AssociateEmergencyContactCommandHandler associateEmergencyContactHandler,
      RegisterOlderAdultProfileCommandHandler profiles,
      GenerateLinkingCodeCommandHandler codes,
      AcceptCareLinkCommandHandler acceptances,
      RegisterConsentCommandHandler consents,
      WithdrawCareLinkConsentCommandHandler withdrawals,
      EndCareLinkCommandHandler ends,
      AdultAccessCodeCommandHandler accessCodes,
      CoCaregiverCommandHandler coCaregivers) {
    this.updateOlderAdultProfileHandler = updateOlderAdultProfileHandler;
    this.associateEmergencyContactHandler = associateEmergencyContactHandler;
    this.profiles = profiles;
    this.codes = codes;
    this.acceptances = acceptances;
    this.consents = consents;
    this.withdrawals = withdrawals;
    this.ends = ends;
    this.accessCodes = accessCodes;
    this.coCaregivers = coCaregivers;
  }

  @Override
  public Result<AdultAccessCodeResult, ApplicationError> inviteCaregiver(
      String careLinkId, String caregiverId) {
    return execute(() -> coCaregivers.invite(careLinkId, caregiverId));
  }

  @Override
  public Result<CareLinkResult, ApplicationError> joinAsCaregiver(
      String caregiverId, String code) {
    return execute(() -> coCaregivers.join(caregiverId, code));
  }

  @Override
  public Result<List<CaregiverRequestResult>, ApplicationError> caregiverRequests(
      String olderAdultId) {
    return execute(() -> coCaregivers.requests(olderAdultId));
  }

  @Override
  public Result<CareLinkResult, ApplicationError> decideCaregiverRequest(
      String olderAdultId, String careLinkId, boolean accepted) {
    return execute(() -> coCaregivers.decide(olderAdultId, careLinkId, accepted));
  }

  @Override
  public Result<ConsentWithdrawalResult, ApplicationError> removeAdultAccount(
      String olderAdultId) {
    return execute(() -> coCaregivers.removeAccount(olderAdultId));
  }

  /** Delegates the profile change to its transactional handler. */
  @Override
  public Result<OlderAdultProfileResult, ApplicationError> updateOlderAdultProfile(
      UpdateOlderAdultProfileCommand command) {
    return execute(() -> updateOlderAdultProfileHandler.handle(command));
  }

  /** Delegates the profile change to its transactional handler. */
  @Override
  public Result<OlderAdultProfileResult, ApplicationError> associateEmergencyContact(
      AssociateEmergencyContactCommand command) {
    return execute(() -> associateEmergencyContactHandler.handle(command));
  }

  /** Registers a profile through its dedicated handler. */
  @Override
  public Result<OlderAdultProfileResult, ApplicationError> registerOlderAdult(
      RegisterOlderAdultProfileCommand command) {
    return execute(() -> profiles.handle(command));
  }

  /** Issues pending linking evidence through its dedicated handler. */
  @Override
  public Result<CareLinkResult, ApplicationError> generateLinkingCode(
      GenerateLinkingCodeCommand command) {
    return execute(() -> codes.handle(command));
  }

  /** Accepts linking evidence through its dedicated handler. */
  @Override
  public Result<CareLinkResult, ApplicationError> accept(AcceptCareLinkCommand command) {
    return execute(() -> acceptances.handle(command));
  }

  /** Returns the decision outcome observed while holding the relationship lock. */
  @Override
  public Result<ConsentDecisionResult, ApplicationError> registerConsentDecision(
      RegisterConsentCommand command) {
    return execute(() -> consents.handleDecision(command));
  }

  /** Records consent through its dedicated handler. */
  @Override
  public Result<CareLinkResult, ApplicationError> registerConsent(RegisterConsentCommand command) {
    return execute(() -> consents.handle(command));
  }

  /** Withdraws the adult decision through its dedicated transactional handler. */
  @Override
  public Result<ConsentWithdrawalResult, ApplicationError> withdrawConsent(
      WithdrawCareLinkConsentCommand command) {
    return execute(() -> withdrawals.handle(command));
  }

  /** Ends the caregiver's own relationship. */
  @Override
  public Result<ConsentWithdrawalResult, ApplicationError> end(EndCareLinkCommand command) {
    return execute(() -> ends.handle(command));
  }

  /** Issues a sign-in code for the adult. */
  @Override
  public Result<AdultAccessCodeResult, ApplicationError> issueAccessCode(
      IssueAdultAccessCodeCommand command) {
    return execute(() -> accessCodes.issue(command));
  }

  /** Redeems a sign-in code on the adult's device. */
  @Override
  public Result<AdultAccessRedemptionResult, ApplicationError> redeemAccessCode(
      RedeemAdultAccessCodeCommand command) {
    return execute(() -> accessCodes.redeem(command));
  }

  /** Captures expected rejection after the transactional handler has rolled back. */
  private <T> Result<T, ApplicationError> execute(Supplier<T> operation) {
    try {
      return Result.success(operation.get());
    } catch (CareLinkApplicationException failure) {
      return Result.failure(failure.toApplicationError());
    }
  }
}
