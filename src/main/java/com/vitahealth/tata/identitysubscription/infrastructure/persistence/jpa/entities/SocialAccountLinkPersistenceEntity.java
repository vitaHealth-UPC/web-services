package com.vitahealth.tata.identitysubscription.infrastructure.persistence.jpa.entities;

import jakarta.persistence.*;
import java.util.UUID;

/** Immutable external identity association; never persists a Firebase bearer credential. */
@Entity
@Table(name = "identity_social_links",
    uniqueConstraints = @UniqueConstraint(name = "uk_identity_social_issuer_subject",
        columnNames = {"issuer", "external_subject"}))
public class SocialAccountLinkPersistenceEntity {
  @Id @Column(length = 36)
  private String id;

  @Column(nullable = false, length = 255)
  private String issuer;

  @Column(name = "external_subject", nullable = false, length = 128)
  private String subject;

  @Column(name = "account_id", nullable = false, length = 255)
  private String accountId;

  @Column(nullable = false, length = 16)
  private String provider;

  protected SocialAccountLinkPersistenceEntity() {}

  public SocialAccountLinkPersistenceEntity(String issuer, String subject,
      String accountId, String provider) {
    this.id = UUID.randomUUID().toString();
    this.issuer = issuer;
    this.subject = subject;
    this.accountId = accountId;
    this.provider = provider;
  }

  public String getIssuer() { return issuer; }
  public String getSubject() { return subject; }
  public String getAccountId() { return accountId; }
  public String getProvider() { return provider; }
}
