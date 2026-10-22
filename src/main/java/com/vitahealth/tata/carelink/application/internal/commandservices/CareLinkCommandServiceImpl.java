package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.commandservices.CareLinkCommandService;
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
      WithdrawCareLinkConsentCommandHandler withdrawals) {
    this.updateOlderAdultProfileHandler = updateOlderAdultProfileHandler;
    this.associateEmergencyContactHandler = associateEmergencyContactHandler;
    this.profiles = profiles;
    this.codes = codes;
    this.acceptances = acceptances;
    this.consents = consents;
    this.withdrawals = withdrawals;
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

  /** Captures expected rejection after the transactional handler has rolled back. */
  private <T> Result<T, ApplicationError> execute(Supplier<T> operation) {
    try {
      return Result.success(operation.get());
    } catch (CareLinkApplicationException failure) {
      return Result.failure(failure.toApplicationError());
    }
  }
}
