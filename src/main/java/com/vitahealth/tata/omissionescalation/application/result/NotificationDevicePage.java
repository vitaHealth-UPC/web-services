package com.vitahealth.tata.omissionescalation.application.result;

import java.util.List;

/** One authorized address page; null cursor ends this recipient's device scan. */
public record NotificationDevicePage(List<NotificationDeviceAddress> devices, String nextCursor) {
  /** Retains an immutable authorization snapshot. */
  public NotificationDevicePage {
    devices = List.copyOf(devices);
  }
}
