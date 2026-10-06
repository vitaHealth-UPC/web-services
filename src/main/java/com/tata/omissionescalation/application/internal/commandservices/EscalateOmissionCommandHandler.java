package com.tata.omissionescalation.application.internal.commandservices;

import com.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.tata.omissionescalation.domain.model.commands.CloseOmissionCaseCommand;
import com.tata.omissionescalation.domain.model.commands.EscalateOmissionCommand;
import com.tata.omissionescalation.domain.model.events.EscalationExecuted;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.tata.omissionescalation.domain.services.EscalationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EscalateOmissionCommandHandler {

  static final String ESCALATION_REASON = "Omitted intake still unresolved";

  private final IOmissionCaseRepository repository;
  private final IDomainEventPublisher eventPublisher;
  private final CloseOmissionCaseCommandHandler closeHandler;
  private final EscalationPolicy escalationPolicy = new EscalationPolicy();

  public EscalateOmissionCommandHandler(
      IOmissionCaseRepository repository,
      IDomainEventPublisher eventPublisher,
      CloseOmissionCaseCommandHandler closeHandler) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.closeHandler = closeHandler;
  }

  /** Raises the level when the policy says so, and closes the case once the top level is reached. */
  @Transactional
  public void handle(EscalateOmissionCommand command) {
    OmissionCase omissionCase = repository.findById(command.omissionCaseId()).orElse(null);
    if (omissionCase == null || !escalationPolicy.shouldEscalate(omissionCase, command.now())) {
      return;
    }
    omissionCase.escalate(ESCALATION_REASON, command.now());
    repository.save(omissionCase);
    eventPublisher.publish(new EscalationExecuted(
        omissionCase.getId(),
        omissionCase.getIntakeId(),
        omissionCase.getOlderAdultId(),
        omissionCase.currentLevel().value(),
        command.now()));
    if (omissionCase.currentLevel().isMax()) {
      closeHandler.handle(new CloseOmissionCaseCommand(omissionCase.getId(), command.now()));
    }
  }
}
