package com.vitahealth.tata.carelink.application.internal.outboundservices;

/** Produces candidate codes; issuance coordination and database constraints own uniqueness. */
public interface LinkingCodeGenerator {
  /** Returns a candidate without persisting or reserving any relationship. */
  String generate();
}
