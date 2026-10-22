package com.vitahealth.tata.adherenceanalytics.application.internal.outboundservices;

import com.vitahealth.tata.adherenceanalytics.domain.model.events.AdherencePatternDetected;

/** Publishes stored pattern evidence synchronously within the snapshot creation transaction. */
public interface AdherenceEventPublisher {
  /** Propagates consumer failure so source evidence and projections roll back together. */
  void publish(AdherencePatternDetected event);
}
