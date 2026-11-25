package com.vitahealth.tata.carelink.domain.repositories;

import com.vitahealth.tata.carelink.domain.model.aggregates.AdultAccessCode;
import java.util.Optional;

/** Stores adult access codes; a new code for a relationship replaces its unused ones. */
public interface AdultAccessCodeRepository {
  /** Persists the code state. */
  AdultAccessCode save(AdultAccessCode code);

  /** Reserves a code by value until the redeeming transaction ends. */
  Optional<AdultAccessCode> findByCodeForUpdate(String code);

  /** Reports whether a value is already used by any stored code. */
  boolean exists(String code);

  /** Removes unused codes of the relationship so only the newest one works. */
  void deleteUnusedByCareLinkId(String careLinkId);

  /** Removes unused codes of one kind for a relationship. */
  void deleteUnused(String careLinkId, AdultAccessCode.Kind kind);
}
