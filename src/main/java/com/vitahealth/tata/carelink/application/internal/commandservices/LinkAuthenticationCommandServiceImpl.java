package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.commandservices.LinkAuthenticationCommandService;
import com.vitahealth.tata.carelink.application.result.AuthenticatedCareLinkResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.commands.RegisterConsentCommand;
import com.vitahealth.tata.shared.application.result.ApplicationError;
import com.vitahealth.tata.shared.application.result.Result;
import java.util.function.Supplier;
import org.springframework.stereotype.Service;

/** Exposes onboarding commands through an independent transactional coordinator. */
@Service
public class LinkAuthenticationCommandServiceImpl implements LinkAuthenticationCommandService {
  private final LinkAuthenticationCommandHandler handler;

  /** Injects the independent transaction boundary for relationship and session changes. */
  public LinkAuthenticationCommandServiceImpl(LinkAuthenticationCommandHandler handler) {
    this.handler = handler;
  }

  /** Accepts the linking code and issues the restricted onboarding credential atomically. */
  @Override
  public Result<AuthenticatedCareLinkResult, ApplicationError> accept(
      AcceptCareLinkCommand command) {
    return execute(() -> handler.accept(command));
  }

  /** Issues adult access only for first approval; rejection and replay return no new credential. */
  @Override
  public Result<AuthenticatedCareLinkResult, ApplicationError> registerConsent(
      RegisterConsentCommand command) {
    return execute(() -> handler.registerConsent(command));
  }

  /** Converts expected rejection only after the coordinator transaction has completed. */
  private Result<AuthenticatedCareLinkResult, ApplicationError> execute(
      Supplier<AuthenticatedCareLinkResult> operation) {
    try {
      return Result.success(operation.get());
    } catch (CareLinkApplicationException failure) {
      return Result.failure(failure.toApplicationError());
    }
  }
}
