package com.vitahealth.tata.identitysubscription.application.internal.outboundservices;

/** Supplies unpredictable verification codes independently of storage and delivery. */
public interface VerificationCodeGenerator {
  String generate();
}
