package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.application.result.RecordedAlertEventResult;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.RecordedAlertEventResource;

/** Keeps transport serialization outside the timeline query handler. */
public final class RecordedAlertEventResourceAssembler {
  private RecordedAlertEventResourceAssembler() {}

  public static RecordedAlertEventResource from(RecordedAlertEventResult event) {
    return new RecordedAlertEventResource(event.type().name(), event.occurredAt());
  }
}
