package com.vitahealth.tata.identitysubscription.application.result;

import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.AccountStatus;

/** Account lifecycle projection that excludes stored password and verification hashes. */
public record AccountResult(String id, String name, String email, AccountStatus status) {}
