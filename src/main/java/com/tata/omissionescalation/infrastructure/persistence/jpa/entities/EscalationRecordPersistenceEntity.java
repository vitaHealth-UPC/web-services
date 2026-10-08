package com.tata.omissionescalation.infrastructure.persistence.jpa.entities;

import com.tata.omissionescalation.domain.model.valueobjects.EscalationLevel;
import com.tata.omissionescalation.infrastructure.persistence.jpa.embeddables.*;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "escalation_records")
public class EscalationRecordPersistenceEntity {


  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Embedded
  private EscalationLevelPersistenceEmbeddable level;

  @Column(nullable = false)
  private String reason;

  @Column(nullable = false)
  private Instant triggeredAt;


  public EscalationRecordPersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public EscalationLevelPersistenceEmbeddable getLevel() { return level; }
  public void setLevel(EscalationLevelPersistenceEmbeddable value) { this.level = value; }
  public String getReason() { return reason; }
  public void setReason(String value) { this.reason = value; }
  public Instant getTriggeredAt() { return triggeredAt; }
  public void setTriggeredAt(Instant value) { this.triggeredAt = value; }
}
