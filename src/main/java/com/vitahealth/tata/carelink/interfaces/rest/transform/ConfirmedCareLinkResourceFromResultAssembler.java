package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.application.result.ConfirmedCareLinkResult;
import com.vitahealth.tata.carelink.interfaces.rest.resources.ConfirmedCareLinkResource;
import java.util.Objects;

/** Converts the application display projection into the existing credential-free list response. */
public final class ConfirmedCareLinkResourceFromResultAssembler {
  private ConfirmedCareLinkResourceFromResultAssembler() {}

  /** Preserves identity, adult display information and confirmation time without extra queries. */
  public static ConfirmedCareLinkResource toResource(ConfirmedCareLinkResult result) {
    Objects.requireNonNull(result, "result");
    return new ConfirmedCareLinkResource(
        result.id(), result.olderAdultId(), result.olderAdultName(), result.confirmedAt());
  }
}
