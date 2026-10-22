package com.vitahealth.tata.omissionescalation.application.internal.outboundservices;

import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentReceipt;
import com.vitahealth.tata.omissionescalation.application.result.NotificationIntentRequest;

/** Commits one idempotent source-case notification intention with the business transaction. */
public interface NotificationIntentStore {
  /** Requires an active caller transaction; repeats preserve the original intention identity. */
  NotificationIntentReceipt enqueue(NotificationIntentRequest request);
}
