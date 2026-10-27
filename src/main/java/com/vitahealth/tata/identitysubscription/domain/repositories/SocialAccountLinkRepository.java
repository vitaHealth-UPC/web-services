package com.vitahealth.tata.identitysubscription.domain.repositories;

import java.util.Optional;

/** Firebase identity-to-account binding. The issuer and subject are immutable after insertion. */
public interface SocialAccountLinkRepository {
  record Link(String issuer, String subject, String accountId, String provider) {}
  Optional<Link> find(String issuer, String subject);
  void bind(Link link);
}
