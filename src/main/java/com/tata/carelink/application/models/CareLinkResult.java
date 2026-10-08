package com.tata.carelink.application.models;

import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;

import java.time.Instant;

public record CareLinkResult(
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
