package com.vitahealth.tata.omissionescalation.application.internal.outboundservices;

import com.vitahealth.tata.omissionescalation.application.result.NotificationDispatchRequest;

/** Sends one authorized device attempt and returns actual provider acceptance evidence. */
@FunctionalInterface
public interface NotificationDeviceTransport {
  /**
   * Returns a nonblank provider receipt or throws; callers persist only stable failure categories.
   */
  String send(NotificationDispatchRequest request);
}
