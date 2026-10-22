package com.vitahealth.tata.identitysubscription.domain.repositories;

import com.vitahealth.tata.identitysubscription.domain.model.aggregates.PushDeviceRegistration;
import java.util.Optional;

/** Stores installation ownership and token rotation without depending on a push transport. */
public interface PushDeviceRegistrationRepository {
  /** Persists a new registration or a rotation under its original session ownership. */
  PushDeviceRegistration save(PushDeviceRegistration registration);

  /** Returns one detached immutable registration snapshot. */
  Optional<PushDeviceRegistration> findByInstallationId(String installationId);

  /** Reserves an existing installation inside the caller's mutation transaction. */
  Optional<PushDeviceRegistration> findByInstallationIdForUpdate(String installationId);

  /** Removes only the exact reserved session owner inside the caller transaction. */
  void deleteOwned(String installationId, String sessionId);
}
