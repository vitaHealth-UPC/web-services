package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

/** Replaces timing and ordered recipients without changing accessibility or channel choices. */
public record UpdateNotificationPolicyResource(
    @NotNull @Valid NotificationPolicyResource policy,
    @NotNull @Size(max = 5) List<@NotNull @Valid EmergencyContactResource> contacts) {
  /** Copies submitted recipients while leaving absent input to bean validation. */
  public UpdateNotificationPolicyResource {
    if (contacts != null) contacts = List.copyOf(contacts);
  }
}
