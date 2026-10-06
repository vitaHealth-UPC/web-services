package com.tata.carelink.interfaces.rest.resources;

import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;

import java.time.Instant;

public record CareLinkResource(
        String id,
        String caregiverId,
        String olderAdultId,
        CareLinkStatus status,
        String linkingCode,
        Instant codeExpiresAt,
        Instant codeUsedAt,
        boolean consentGranted,
        Instant consentRecordedAt,
        Instant confirmedAt
) {}
