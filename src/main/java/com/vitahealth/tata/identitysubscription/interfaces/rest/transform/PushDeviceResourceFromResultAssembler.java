package com.vitahealth.tata.identitysubscription.interfaces.rest.transform;

import com.vitahealth.tata.identitysubscription.application.result.PushDeviceResult;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.PushDeviceResource;

/** Publishes device confirmation independently of persistence and provider representations. */
public final class PushDeviceResourceFromResultAssembler {
  private PushDeviceResourceFromResultAssembler() {}

  /** Returns only the installation reference and accepted update time. */
  public static PushDeviceResource toResource(PushDeviceResult result) {
    return new PushDeviceResource(result.installationId(), result.updatedAt());
  }
}
