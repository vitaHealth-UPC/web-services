package com.tata.accessibilitypreferences.infrastructure.persistence.jpa.entities;

import com.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import com.tata.accessibilitypreferences.domain.model.valueobjects.TextSizeLevel;
import jakarta.persistence.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_preferences")
public class UserPreferencesPersistenceEntity {
    @Id private String id;
    @Column(name = "user_id", nullable = false, unique = true, length = 64) private String userId;
    @Enumerated(EnumType.STRING) @Column(name = "text_size", nullable = false, length = 20) private TextSizeLevel textSize;
    @Column(name = "high_contrast", nullable = false) private boolean highContrast;
    @Column(name = "reduced_motion", nullable = false) private boolean reducedMotion;
    @Column(name = "reading_assistance", nullable = false) private boolean readingAssistance;
    @Column(name = "voice_confirmation_enabled", nullable = false) private boolean voiceConfirmationEnabled;
    @Column(name = "quiet_hours_start") private LocalTime quietHoursStart;
    @Column(name = "quiet_hours_end") private LocalTime quietHoursEnd;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_notification_channels", joinColumns = @JoinColumn(name = "user_preferences_id"))
    private List<ChannelEmbeddable> channels = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at") private Instant updatedAt;

    protected UserPreferencesPersistenceEntity() {}

    public UserPreferencesPersistenceEntity(
            String id, String userId, TextSizeLevel textSize, boolean highContrast, boolean reducedMotion,
            boolean readingAssistance, boolean voiceConfirmationEnabled, LocalTime quietHoursStart,
            LocalTime quietHoursEnd, List<ChannelEmbeddable> channels, Instant createdAt
    ) {
        this.id = id;
        this.userId = userId;
        this.textSize = textSize;
        this.highContrast = highContrast;
        this.reducedMotion = reducedMotion;
        this.readingAssistance = readingAssistance;
        this.voiceConfirmationEnabled = voiceConfirmationEnabled;
        this.quietHoursStart = quietHoursStart;
        this.quietHoursEnd = quietHoursEnd;
        this.channels = new ArrayList<>(channels);
        this.createdAt = createdAt;
    }

    public String getId() { return id; }
    public String getUserId() { return userId; }
    public TextSizeLevel getTextSize() { return textSize; }
    public boolean isHighContrast() { return highContrast; }
    public boolean isReducedMotion() { return reducedMotion; }
    public boolean isReadingAssistance() { return readingAssistance; }
    public boolean isVoiceConfirmationEnabled() { return voiceConfirmationEnabled; }
    public LocalTime getQuietHoursStart() { return quietHoursStart; }
    public LocalTime getQuietHoursEnd() { return quietHoursEnd; }
    public List<ChannelEmbeddable> getChannels() { return List.copyOf(channels); }
    public Instant getCreatedAt() { return createdAt; }

    @Embeddable
    public static class ChannelEmbeddable {
        @Enumerated(EnumType.STRING)
        @Column(name = "channel_type", nullable = false, length = 10)
        private ChannelType type;

        @Column(name = "enabled", nullable = false)
        private boolean enabled;

        protected ChannelEmbeddable() {}

        public ChannelEmbeddable(ChannelType type, boolean enabled) {
            this.type = type;
            this.enabled = enabled;
        }

        public ChannelType getType() { return type; }
        public boolean isEnabled() { return enabled; }
    }
}
