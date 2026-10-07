package com.tata.familymonitoring.domain.ports;

public interface ICareRelationshipPort {
    boolean isAuthorized(String caregiverId, String olderAdultId);
}
