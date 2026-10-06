package com.tata.carelink.domain.repositories;

import com.tata.carelink.domain.model.aggregates.OlderAdultProfile;

import java.util.Optional;

public interface OlderAdultProfileRepository {
    OlderAdultProfile save(OlderAdultProfile profile);
    Optional<OlderAdultProfile> findById(String id);
}
