package com.vitahealth.tata.identitysubscription.infrastructure.email.resend;

import com.vitahealth.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryRequest;
import java.util.List;

/** Produces stable Spanish verification email content for idempotent retries. */
final class ResendEmailRequestAssembler {
  private ResendEmailRequestAssembler() {}

  static ResendEmailRequest from(String sender, VerificationDeliveryRequest request) {
    return new ResendEmailRequest(
        sender,
        List.of(request.email()),
        "Verifica tu cuenta de Tata",
        "Tu código de verificación es "
            + request.code()
            + ".\nVence el "
            + request.expiresAt()
            + ".\nSi no solicitaste esta cuenta, puedes ignorar este mensaje.");
  }
}
