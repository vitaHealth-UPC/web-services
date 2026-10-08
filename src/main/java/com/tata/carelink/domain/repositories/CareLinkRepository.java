package com.tata.carelink.domain.repositories;

import com.tata.carelink.domain.model.aggregates.CareLink;

import java.util.Optional;

public interface CareLinkRepository {
    CareLink save(CareLink careLink);
    Optional<CareLink> findById(String id);
    Optional<CareLink> findByCode(String code);
    Optional<CareLink> findConfirmed(String caregiverId, String olderAdultId);
    java.util.List<CareLink> findConfirmedByCaregiver(String caregiverId);
}
