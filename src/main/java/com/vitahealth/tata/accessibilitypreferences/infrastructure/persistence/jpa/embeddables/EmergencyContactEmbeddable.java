package com.vitahealth.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Stores one recipient in the owner's ordered contact collection, without granting care access. */
@Embeddable
public class EmergencyContactEmbeddable {
  @Column(name = "contact_name", nullable = false, length = 80)
  private String name;

  @Column(name = "contact_phone", nullable = false, length = 16)
  private String phone;

  /** Required by JPA. */
  protected EmergencyContactEmbeddable() {}

  /**
   * Copies a domain-validated name and normalized phone into their existing persistence columns.
   */
  public EmergencyContactEmbeddable(String name, String phone) {
    this.name = name;
    this.phone = phone;
  }

  /** Returns the stored display name. */
  public String getName() {
    return name;
  }

  /** Returns the normalized phone, including its optional international prefix. */
  public String getPhone() {
    return phone;
  }
}
