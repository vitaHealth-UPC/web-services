package com.vitahealth.tata.identitysubscription.application.result;

import java.util.List;

/** Authorized addresses and a scan cursor; null cursor denotes the end of candidate storage. */
public record PushDevicePageResult(List<PushDeviceAddressResult> devices, String nextCursor) {
  /** Copies the page so consumers cannot mutate the authorization snapshot. */
  public PushDevicePageResult {
    devices = List.copyOf(devices);
  }
}
