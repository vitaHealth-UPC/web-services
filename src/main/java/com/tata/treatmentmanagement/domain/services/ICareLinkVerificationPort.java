package com.tata.treatmentmanagement.domain.services;

@FunctionalInterface
public interface ICareLinkVerificationPort {
    boolean isAuthorized(String caregiverId, String olderAdultId);
}
