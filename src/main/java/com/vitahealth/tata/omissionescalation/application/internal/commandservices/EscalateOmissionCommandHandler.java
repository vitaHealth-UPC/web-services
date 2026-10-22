package com.vitahealth.tata.omissionescalation.application.internal.commandservices;

import com.vitahealth.tata.omissionescalation.application.internal.outboundservices.IDomainEventPublisher;
import com.vitahealth.tata.omissionescalation.domain.model.aggregates.OmissionCase;
import com.vitahealth.tata.omissionescalation.domain.model.commands.CloseOmissionCaseCommand;
import com.vitahealth.tata.omissionescalation.domain.model.commands.EscalateOmissionCommand;
import com.vitahealth.tata.omissionescalation.domain.model.events.EscalationExecuted;
import com.vitahealth.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.vitahealth.tata.omissionescalation.domain.services.EscalationPolicy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Applies the clinical escalation policy under a case reservation and publishes one new level. */
@Service
public class EscalateOmissionCommandHandler {

  static final String ESCALATION_REASON = "Omitted intake still unresolved";

  private final IOmissionCaseRepository repository;
  private final IDomainEventPublisher eventPublisher;
  private final CloseOmissionCaseCommandHandler closeHandler;
  private final EscalationPolicy escalationPolicy = new EscalationPolicy();

  /** Injects reserved case storage, synchronous event publication and final-level closure. */
  public EscalateOmissionCommandHandler(
      IOmissionCaseRepository repository,
      IDomainEventPublisher eventPublisher,
      CloseOmissionCaseCommandHandler closeHandler) {
    this.repository = repository;
    this.eventPublisher = eventPublisher;
    this.closeHandler = closeHandler;
  }

  /**
   * Raises the level when the policy says so, and closes the case once the top level is reached.
   */
  @Transactional
  public void handle(EscalateOmissionCommand command) {
    OmissionCase omissionCase = repository.findByIdForUpdate(command.omissionCaseId()).orElse(null);
    if (omissionCase == null || !escalationPolicy.shouldEscalate(omissionCase, command.now())) {
      return;
    }
    omissionCase.escalate(ESCALATION_REASON, command.now());
    repository.save(omissionCase);
    eventPublisher.publish(
        new EscalationExecuted(
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
