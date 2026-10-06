package com.tata.carelink.infrastructure.persistence.jpa.entities;

import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
        name = "care_links",
        indexes = {
                @Index(name = "idx_care_links_code", columnList = "linking_code"),
                @Index(name = "idx_care_links_members", columnList = "caregiver_id,older_adult_id")
        }
)
public class CareLinkPersistenceEntity {
    @Id private String id;
    @Column(name = "caregiver_id", nullable = false) private String caregiverId;
    @Column(name = "older_adult_id", nullable = false) private String olderAdultId;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40) private CareLinkStatus status;
    @Column(name = "linking_code", nullable = false, length = 32) private String linkingCode;
    @Column(name = "code_expires_at", nullable = false) private Instant codeExpiresAt;
    @Column(name = "code_used_at") private Instant codeUsedAt;
    @Column(name = "consent_granted", nullable = false) private boolean consentGranted;
    @Column(name = "consent_recorded_at") private Instant consentRecordedAt;
    @Column(name = "confirmed_at") private Instant confirmedAt;
    @Column(name = "created_at", nullable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    protected CareLinkPersistenceEntity() {}

    public CareLinkPersistenceEntity(
            String id,
            String caregiverId,
            String olderAdultId,
            CareLinkStatus status,
            String linkingCode,
            Instant codeExpiresAt,
            Instant codeUsedAt,
            boolean consentGranted,
            Instant consentRecordedAt,
            Instant confirmedAt,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.id = id;
        this.caregiverId = caregiverId;
        this.olderAdultId = olderAdultId;
        this.status = status;
        this.linkingCode = linkingCode;
        this.codeExpiresAt = codeExpiresAt;
        this.codeUsedAt = codeUsedAt;
        this.consentGranted = consentGranted;
        this.consentRecordedAt = consentRecordedAt;
        this.confirmedAt = confirmedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public String getCaregiverId() { return caregiverId; }
    public String getOlderAdultId() { return olderAdultId; }
    public CareLinkStatus getStatus() { return status; }
    public String getLinkingCode() { return linkingCode; }
    public Instant getCodeExpiresAt() { return codeExpiresAt; }
    public Instant getCodeUsedAt() { return codeUsedAt; }
    public boolean isConsentGranted() { return consentGranted; }
    public Instant getConsentRecordedAt() { return consentRecordedAt; }
    public Instant getConfirmedAt() { return confirmedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
