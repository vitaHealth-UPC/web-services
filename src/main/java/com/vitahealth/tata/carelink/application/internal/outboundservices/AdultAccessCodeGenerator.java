package com.vitahealth.tata.carelink.application.internal.outboundservices;

/** Produces hard-to-guess candidate access codes; issuance owns uniqueness. */
public interface AdultAccessCodeGenerator {
  /** Returns one candidate value. */
  String generate();
}
