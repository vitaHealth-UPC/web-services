package com.vitahealth.tata.identitysubscription.domain.model.valueobjects;

/**
 * Product capabilities associated with catalog plans and exposed through subscription projections.
 */
public enum PlanCapability {
  /** Medication reminder functionality. */
  REMINDERS,
  /** Scheduled intake agenda functionality. */
  AGENDA,
  /** Recording definitive intake confirmations. */
  INTAKE_CONFIRMATION,
  /** Family alert functionality. */
  FAMILY_ALERTS,
  /** Consented family monitoring functionality. */
  FAMILY_MONITORING,
  /** Adherence summaries and evidence-based insights. */
  ADHERENCE_INSIGHTS
}
