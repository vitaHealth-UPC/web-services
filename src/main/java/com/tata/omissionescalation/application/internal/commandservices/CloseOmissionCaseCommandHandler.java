package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.domain.model.commands.CloseOmissionCaseCommand;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CloseOmissionCaseCommandHandler {

  private final IOmissionCaseRepository repository;

  public CloseOmissionCaseCommandHandler(IOmissionCaseRepository repository) {
    this.repository = repository;
  }

  /** Closes the case keeping its alerts and escalation history. */
  @Transactional
  public void handle(CloseOmissionCaseCommand command) {
    repository.findByIdForUpdate(command.omissionCaseId())
        .filter(c -> c.getStatus() == OmissionCaseStatus.OMITTED
            || c.getStatus() == OmissionCaseStatus.ESCALATED)
        .ifPresent(c -> {
          c.close(command.now());
          repository.save(c);
        });
  }
}
