package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.adapters;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.accessibilitypreferences.application.internal.outboundservices.PreferencesMutationLock;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities.PreferencesMutationLockEntity;
import com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.repositories.PreferencesMutationLockJpaRepository;
import com.vitahealth.tata.shared.infrastructure.persistence.jpa.exceptions.UniqueConstraintClassifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

/** Coordinates one user's preferences through a database key shared by application instances. */
@Component
public class JpaPreferencesMutationLock implements PreferencesMutationLock {
  private final PreferencesMutationLockJpaRepository keys;
  private final PlatformTransactionManager transactions;

  /** Injects key persistence and explicit technical initialization transaction boundaries. */
  public JpaPreferencesMutationLock(
      PreferencesMutationLockJpaRepository keys, PlatformTransactionManager transactions) {
    this.keys = keys;
    this.transactions = transactions;
  }

  /**
   * Creates only a technical key independently, then reserves it in the actual command transaction.
   */
  @Override
  @Transactional(propagation = Propagation.MANDATORY)
  public void reserve(String userId) {
    var owner = requireText(userId, "userId");
    if (owner.length() > 64)
      throw new IllegalArgumentException("userId exceeds the supported length");
    var initialization = new TransactionTemplate(transactions);
    initialization.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    try {
      initialization.executeWithoutResult(
          status -> {
            if (keys.findByUserId(owner).isEmpty())
              keys.saveAndFlush(new PreferencesMutationLockEntity(owner));
          });
    } catch (DataIntegrityViolationException conflict) {
      if (!UniqueConstraintClassifier.matches(conflict, "uk_preferences_mutation_user"))
        throw conflict;
    }
    keys.findForUpdate(owner)
        .orElseThrow(
            () -> new IllegalStateException("preference reservation could not be initialized"));
  }
}
