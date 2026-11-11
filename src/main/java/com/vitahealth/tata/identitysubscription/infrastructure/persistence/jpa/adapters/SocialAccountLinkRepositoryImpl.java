package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.adapters;

import com.vitahealth.tata.identitysubscription.domain.repositories.SocialAccountLinkRepository;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities.SocialAccountLinkPersistenceEntity;
import com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.repositories.SocialAccountLinkJpaRepository;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** Isolates JPA writes from the application and requires a surrounding account transaction. */
@Repository
public class SocialAccountLinkRepositoryImpl implements SocialAccountLinkRepository {
  private final SocialAccountLinkJpaRepository jpa;
  public SocialAccountLinkRepositoryImpl(SocialAccountLinkJpaRepository jpa) { this.jpa = jpa; }

  @Override public Optional<Link> find(String issuer, String subject) {
    return jpa.findByIssuerAndSubject(issuer, subject)
        .map(row -> new Link(row.getIssuer(), row.getSubject(), row.getAccountId(), row.getProvider()));
  }

  @Override @Transactional
  public void bind(Link link) {
    jpa.saveAndFlush(new SocialAccountLinkPersistenceEntity(
        link.issuer(), link.subject(), link.accountId(), link.provider()));
  }
}
