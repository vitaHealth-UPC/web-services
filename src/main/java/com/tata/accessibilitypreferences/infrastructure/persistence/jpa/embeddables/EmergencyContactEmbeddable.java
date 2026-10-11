package com.tata.accessibilitypreferences.infrastructure.persistence.jpa.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class EmergencyContactEmbeddable {
  @Column(name = "contact_name", nullable = false, length = 80)
  private String name;

  @Column(name = "contact_phone", nullable = false, length = 16)
  private String phone;

  protected EmergencyContactEmbeddable() {}

  public EmergencyContactEmbeddable(String name, String phone) {
    this.name = name;
    this.phone = phone;
  }

  public String getName() {
    return name;
  }

  public String getPhone() {
    return phone;
  }
}
