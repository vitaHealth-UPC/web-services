package com.tata.identitysubscription.domain.repositories;

import com.tata.identitysubscription.domain.model.aggregates.PinCredential;
import java.util.Optional;

public interface PinCredentialRepository {
    Optional<PinCredential> findByOlderAdultId(String olderAdultId);
    default java.util.Optional<PinCredential> findForAuthentication(String olderAdultId) { return findByOlderAdultId(olderAdultId); }
    PinCredential save(PinCredential credential);
}
