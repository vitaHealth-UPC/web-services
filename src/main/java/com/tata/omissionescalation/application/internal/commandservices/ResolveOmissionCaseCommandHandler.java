package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.domain.model.commands.ResolveOmissionCaseCommand;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResolveOmissionCaseCommandHandler {

  private final IOmissionCaseRepository repository;

  public ResolveOmissionCaseCommandHandler(IOmissionCaseRepository repository) {
    this.repository = repository;
  }

  /** Resolves the case only when the confirmation arrives during the grace period. */
  @Transactional
  public void handle(ResolveOmissionCaseCommand command) {
    Instant now = command.confirmedAt();
    repository.findByIntakeId(command.intakeId())
        .filter(c -> c.getStatus() == OmissionCaseStatus.PENDING)
        .filter(c -> c.getGracePeriod().isActive(now))
        .ifPresent(c -> {
          c.resolve(now);
          repository.save(c);
        });
  }
}
