package com.vitahealth.tata.omissionescalation.application.result;

import java.util.List;

/** One credential-free caregiver recipient page for an omitted adult intake. */
public record NotificationCaregiverPage(List<String> caregiverIds, String nextCursor) {
  /** Copies the caregiver scan so later consumers cannot alter its membership. */
  public NotificationCaregiverPage {
    caregiverIds = List.copyOf(caregiverIds);
  }
}
