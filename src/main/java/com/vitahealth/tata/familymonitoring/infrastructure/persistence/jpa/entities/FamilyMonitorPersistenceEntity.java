package com.vitahealth.tata.familymonitoring.infrastructure.persistence.jpa.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/** Stores versioned relationship follow-up and the identities of its owned evidence. */
@Entity
@Table(
    name = "family_monitors",
    uniqueConstraints =
        @UniqueConstraint(name = "uq_family_monitor_care_link", columnNames = "care_link_id"))
public class FamilyMonitorPersistenceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(nullable = false)
  private Long revision;

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

  @ElementCollection
  @CollectionTable(
      name = "family_monitor_inventory_positions",
      joinColumns = @JoinColumn(name = "family_monitor_id"),
      uniqueConstraints =
          @UniqueConstraint(
              name = "uq_family_inventory_position",
              columnNames = {"family_monitor_id", "medication_id"}))
  private List<InventoryStockPositionEmbeddable> inventoryStockPositions = new ArrayList<>();

  @Column(updatable = false)
  private Instant createdAt;

  private Instant updatedAt;

  /** Returns managed source positions retained after shortage resolution. */
  public List<InventoryStockPositionEmbeddable> getInventoryStockPositions() {
    return inventoryStockPositions;
  }

  /** Copies complete source positions while assembling a detached persistence snapshot. */
  public void setInventoryStockPositions(List<InventoryStockPositionEmbeddable> positions) {
    inventoryStockPositions = new ArrayList<>(positions);
  }

  /** Required by the persistence provider. */
  public FamilyMonitorPersistenceEntity() {}

  /** Returns the parent revision used to reject stale aggregate snapshots. */
  public Long getRevision() {
    return revision;
  }

  /** Copies observed revision during persistence snapshot assembly. */
  public void setRevision(Long revision) {
    this.revision = revision;
  }

  /** Returns stored row identity. */
  public Long getId() {
    return id;
  }

  /** Copies stored row identity during managed persistence assembly or update. */
  public void setId(Long value) {
    this.id = value;
  }

  /** Returns exact care relationship reference. */
  public String getCareLinkId() {
    return careLinkId;
  }

  /** Copies exact care relationship reference during managed persistence assembly or update. */
  public void setCareLinkId(String value) {
    this.careLinkId = value;
  }

  /** Returns logical adult owner reference. */
  public String getOlderAdultId() {
    return olderAdultId;
  }

  /** Copies logical adult owner reference during managed persistence assembly or update. */
  public void setOlderAdultId(String value) {
    this.olderAdultId = value;
  }

  /** Returns caregiver author or relationship owner reference. */
  public String getFamiliarId() {
    return familiarId;
  }

  /**
   * Copies caregiver author or relationship owner reference during managed persistence assembly or
   * update.
   */
  public void setFamiliarId(String value) {
    this.familiarId = value;
  }

  /** Returns managed owned alert history. */
  public List<AlertSummaryPersistenceEntity> getAlerts() {
    return alerts;
  }

  /** Copies managed owned alert history during managed persistence assembly or update. */
  public void setAlerts(List<AlertSummaryPersistenceEntity> value) {
    this.alerts = value;
  }

  /** Returns managed owned intervention history. */
  public List<CaregiverNotePersistenceEntity> getNotes() {
    return notes;
  }

  /** Copies managed owned intervention history during managed persistence assembly or update. */
  public void setNotes(List<CaregiverNotePersistenceEntity> value) {
    this.notes = value;
  }

  /** Returns managed unresolved source shortage evidence. */
  public List<LowStockNoticePersistenceEntity> getLowStockNotices() {
    return lowStockNotices;
  }

  /**
   * Copies managed unresolved source shortage evidence during managed persistence assembly or
   * update.
   */
  public void setLowStockNotices(List<LowStockNoticePersistenceEntity> value) {
    this.lowStockNotices = value;
  }

  /** Returns managed owned analytics patterns. */
  public List<AdherenceInsightPersistenceEntity> getAdherenceInsights() {
    return adherenceInsights;
  }

  /** Copies managed owned analytics patterns during managed persistence assembly or update. */
  public void setAdherenceInsights(List<AdherenceInsightPersistenceEntity> value) {
    this.adherenceInsights = value;
  }

  /** Returns original aggregate persistence audit instant. */
  public Instant getCreatedAt() {
    return createdAt;
  }

  /**
   * Copies original aggregate persistence audit instant during managed persistence assembly or
   * update.
   */
  public void setCreatedAt(Instant value) {
    this.createdAt = value;
  }

  /** Returns latest aggregate persistence audit instant. */
  public Instant getUpdatedAt() {
    return updatedAt;
  }

  /**
   * Copies latest aggregate persistence audit instant during managed persistence assembly or
   * update.
   */
  public void setUpdatedAt(Instant value) {
    this.updatedAt = value;
  }
}
