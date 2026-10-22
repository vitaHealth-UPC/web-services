package com.vitahealth.tata.identitysubscription.infrastructure.email.resend;

import java.util.List;

/** Resend's text email payload, confined to its infrastructure adapter. */
record ResendEmailRequest(String from, List<String> to, String subject, String text) {
  /** Prevents email addresses and verification codes from appearing in diagnostics. */
  @Override
  public String toString() {
    return "ResendEmailRequest[redacted]";
  }
}
