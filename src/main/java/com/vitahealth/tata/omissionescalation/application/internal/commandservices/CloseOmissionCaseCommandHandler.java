package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.domain.model.commands.CloseOmissionCaseCommand;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Closes active omission cases under the same row reservation used by escalation and delivery. */
@Service
public class CloseOmissionCaseCommandHandler {

  private final IOmissionCaseRepository repository;

  /** Injects the context repository that refreshes reserved case evidence. */
  public CloseOmissionCaseCommandHandler(IOmissionCaseRepository repository) {
    this.repository = repository;
  }

  /**
   * Closes the reserved current case, retaining alerts and escalation history; repeated closure is
   * a no-op.
   */
  @Transactional
  public void handle(CloseOmissionCaseCommand command) {
    repository
        .findByIdForUpdate(command.omissionCaseId())
        .filter(
            c ->
                c.getStatus() == OmissionCaseStatus.OMITTED
                    || c.getStatus() == OmissionCaseStatus.ESCALATED)
        .ifPresent(
            c -> {
              c.close(command.now());
              repository.save(c);
            });
  }
}
