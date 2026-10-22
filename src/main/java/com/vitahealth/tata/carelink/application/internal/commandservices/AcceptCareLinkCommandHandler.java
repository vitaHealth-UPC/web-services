package com.vitahealth.tata.carelink.application.internal.commandservices;

import com.vitahealth.tata.carelink.application.CareLinkApplicationException;
import com.vitahealth.tata.carelink.application.internal.CareLinkMapper;
import com.vitahealth.tata.carelink.application.internal.authorization.EnabledCaregiverAccountGuard;
import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import java.time.Clock;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Consumes valid linking evidence without combining adult profile or consent commands. */
@Service
@Transactional
public class AcceptCareLinkCommandHandler {
  private final CareLinkRepository links;
  private final EnabledCaregiverAccountGuard caregivers;
  private final Clock clock;

  /** Injects link persistence, current caregiver eligibility and the shared time source. */
  public AcceptCareLinkCommandHandler(
      CareLinkRepository links, EnabledCaregiverAccountGuard caregivers, Clock clock) {
    this.links = links;
    this.caregivers = caregivers;
    this.clock = clock;
  }

  /** Matches the caller and supplied code before moving the relationship to consent onboarding. */
  public CareLinkResult handle(AcceptCareLinkCommand command) {
    caregivers.requireEnabled(command.caregiverId());
    if (command.code() == null || command.code().isBlank())
      throw invalidCode("linking code is required");
    var normalized = command.code().strip().toUpperCase(Locale.ROOT);
    if (normalized.length() > LinkingCode.MAX_LENGTH) throw invalidCode("linking code is invalid");
    var link =
        links
            .findByCodeForUpdate(normalized)
            .orElseThrow(() -> invalidCode("linking code is invalid"));
    if (!link.caregiverId().equals(command.caregiverId()))
      throw invalidCode("linking code is invalid");
    try {
      link.accept(command.code(), clock.instant());
    } catch (IllegalStateException exception) {
      throw new CareLinkApplicationException(
          CareLinkApplicationException.Code.LINKING_CODE_EXPIRED_OR_USED, exception.getMessage());
    } catch (IllegalArgumentException exception) {
      throw invalidCode(exception.getMessage());
    }
    return CareLinkMapper.toResult(links.save(link));
  }

  private static CareLinkApplicationException invalidCode(String message) {
    return new CareLinkApplicationException(
        CareLinkApplicationException.Code.INVALID_LINKING_CODE, message);
  }
}
