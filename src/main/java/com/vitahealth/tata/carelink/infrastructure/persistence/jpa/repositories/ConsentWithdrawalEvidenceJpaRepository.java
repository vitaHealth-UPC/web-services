package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories;

import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.ConsentWithdrawalEvidenceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

/** Stores context-owned withdrawal evidence without modifying the historical decision columns. */
public interface ConsentWithdrawalEvidenceJpaRepository
    extends JpaRepository<ConsentWithdrawalEvidenceEntity, String> {}
