package com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;

/** Provides a database reservation shared by every issuer of the short linking-code namespace. */
@Entity
@Table(
    name = "care_link_code_namespaces",
    uniqueConstraints =
        @UniqueConstraint(name = "uk_care_link_code_namespace", columnNames = "code_namespace"))
public class CareLinkCodeNamespaceEntity {
  @Id
  @Column(length = 36)
  private String id;

  @Column(name = "code_namespace", nullable = false, updatable = false, length = 32)
  private String namespace;

  /** Constructor required by JPA hydration. */
  protected CareLinkCodeNamespaceEntity() {}

  /** Creates the single coordination resource independently of relationship identities. */
  public CareLinkCodeNamespaceEntity(String namespace) {
    this.id = UUID.randomUUID().toString();
    this.namespace = namespace;
  }
}
