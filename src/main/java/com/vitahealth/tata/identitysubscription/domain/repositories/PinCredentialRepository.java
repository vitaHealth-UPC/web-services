package com.vitahealth.tata.identitysubscription.domain.repositories;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PinCredential;
import java.util.Optional;

/** Persists PIN credentials and serializes updates of failed-attempt state. */
public interface PinCredentialRepository {
  /** Returns credential state for the adult without requesting a mutation lock. */
  Optional<PinCredential> findByOlderAdultId(String olderAdultId);

  /** Requires a transaction and keeps the credential locked until that transaction completes. */
  Optional<PinCredential> findForAuthentication(String olderAdultId);

  /** Persists encoded credential state and its failed-attempt history. */
  PinCredential save(PinCredential credential);
}
