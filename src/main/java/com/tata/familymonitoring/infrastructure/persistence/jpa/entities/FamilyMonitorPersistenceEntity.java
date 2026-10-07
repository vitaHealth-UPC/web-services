package com.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import com.tata.familymonitoring.domain.exceptions.AlertNotFoundException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(name = "family_monitors")
public class FamilyMonitorPersistenceEntity {


  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 36)
  private String careLinkId;

  @Column(nullable = false, length = 36)
  private String olderAdultId;

  @Column(nullable = false, length = 36)
  private String familiarId;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<AlertSummaryPersistenceEntity> alerts = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<CaregiverNotePersistenceEntity> notes = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<LowStockNoticePersistenceEntity> lowStockNotices = new ArrayList<>();

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "family_monitor_id")
  private List<AdherenceInsightPersistenceEntity> adherenceInsights = new ArrayList<>();

  @CreationTimestamp
  @Column(updatable = false)
  private Instant createdAt;

  @UpdateTimestamp
  private Instant updatedAt;


  public FamilyMonitorPersistenceEntity() {}
  public Long getId() { return id; }
  public void setId(Long value) { this.id = value; }
  public String getCareLinkId() { return careLinkId; }
  public void setCareLinkId(String value) { this.careLinkId = value; }
  public String getOlderAdultId() { return olderAdultId; }
  public void setOlderAdultId(String value) { this.olderAdultId = value; }
  public String getFamiliarId() { return familiarId; }
  public void setFamiliarId(String value) { this.familiarId = value; }
  public List<AlertSummaryPersistenceEntity> getAlerts() { return alerts; }
  public void setAlerts(List<AlertSummaryPersistenceEntity> value) { this.alerts = value; }
  public List<CaregiverNotePersistenceEntity> getNotes() { return notes; }
  public void setNotes(List<CaregiverNotePersistenceEntity> value) { this.notes = value; }
  public List<LowStockNoticePersistenceEntity> getLowStockNotices() { return lowStockNotices; }
  public void setLowStockNotices(List<LowStockNoticePersistenceEntity> value) { this.lowStockNotices = value; }
  public List<AdherenceInsightPersistenceEntity> getAdherenceInsights() { return adherenceInsights; }
  public void setAdherenceInsights(List<AdherenceInsightPersistenceEntity> value) { this.adherenceInsights = value; }
  public Instant getCreatedAt() { return createdAt; }
  public void setCreatedAt(Instant value) { this.createdAt = value; }
  public Instant getUpdatedAt() { return updatedAt; }
  public void setUpdatedAt(Instant value) { this.updatedAt = value; }
}
