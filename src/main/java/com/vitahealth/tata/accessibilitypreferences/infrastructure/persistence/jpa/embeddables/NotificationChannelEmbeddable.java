package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables;

import com.vitahealth.tata.accessibilitypreferences.domain.model.valueobjects.ChannelType;
import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

/** Stores one channel choice in the preference owner's existing collection table. */
@Embeddable
public class NotificationChannelEmbeddable {
  @Enumerated(EnumType.STRING)
  @Column(name = "channel_type", nullable = false, length = 10)
  private ChannelType type;

  @Column(name = "enabled", nullable = false)
  private boolean enabled;

  /** Required by JPA. */
  protected NotificationChannelEmbeddable() {}

  /** Copies a channel choice already validated by the domain. */
  public NotificationChannelEmbeddable(ChannelType type, boolean enabled) {
    this.type = type;
    this.enabled = enabled;
  }

  /** Returns the stored channel type. */
  public ChannelType getType() {
    return type;
  }

  /** Returns the explicit stored delivery choice. */
  public boolean isEnabled() {
    return enabled;
  }
}
