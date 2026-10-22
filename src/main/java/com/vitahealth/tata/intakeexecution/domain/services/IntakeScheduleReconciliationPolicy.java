package com.vitahealth.tata.intakeexecution.domain.services;

import static com.vitahealth.tata.shared.domain.validation.DomainText.requireText;

import com.vitahealth.tata.intakeexecution.domain.model.aggregates.Intake;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.IntakeScheduleChangeSet;
import com.vitahealth.tata.intakeexecution.domain.model.valueobjects.ScheduledIntakeSlot;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

/** Reconciles one explicit horizon without mutating supplied aggregates or clinical history. */
public final class IntakeScheduleReconciliationPolicy {
  private IntakeScheduleReconciliationPolicy() {}

  /**
   * Retains pending identity and definitive outcomes, and cancels only obsolete pending slots
   * inside the half-open horizon. Existing evidence outside that horizon is untouched.
   */
  public static IntakeScheduleChangeSet reconcile(
      String treatmentId,
      String olderAdultId,
      List<Intake> existing,
      List<ScheduledIntakeSlot> expected,
      Instant from,
      Instant to) {
    var treatment = requireText(treatmentId, "treatmentId");
    var adult = requireText(olderAdultId, "olderAdultId");
    Objects.requireNonNull(from, "from");
    Objects.requireNonNull(to, "to");
    if (!to.isAfter(from))
      throw new IllegalArgumentException("reconciliation requires an ordered horizon");
    var current = new HashMap<Instant, Intake>();
    for (var intake : List.copyOf(existing)) {
      if (!treatment.equals(intake.treatmentId()) || !adult.equals(intake.olderAdultId()))
        throw new IllegalStateException("stored intake belongs to another schedule owner");
      if (current.putIfAbsent(intake.scheduledAt(), intake) != null)
        throw new IllegalStateException("duplicate intake slots require diagnosis");
    }
    var desired = new TreeMap<Instant, ScheduledIntakeSlot>();
    for (var slot : List.copyOf(expected)) {
      if (!treatment.equals(slot.treatmentId()) || !adult.equals(slot.olderAdultId()))
        throw new IllegalArgumentException("expected slot belongs to another schedule owner");
      if (slot.scheduledAt().isBefore(from) || !slot.scheduledAt().isBefore(to))
        throw new IllegalArgumentException("expected slot is outside the reconciliation horizon");
      if (desired.putIfAbsent(slot.scheduledAt(), slot) != null)
        throw new IllegalArgumentException("expected intake slots must be distinct");
    }
    var pending = new ArrayList<Intake>();
    for (var slot : desired.values()) {
      var intake = current.remove(slot.scheduledAt());
      if (intake != null && !intake.isPending()) continue;
      if (intake == null) {
        pending.add(
            Intake.createScheduled(
                treatment,
                slot.medicationId(),
                adult,
                slot.medication(),
                slot.scheduledAt(),
                from));
      } else {
        pending.add(
            Intake.rehydrate(
                intake.id(),
                treatment,
                slot.medicationId(),
                adult,
                slot.medication(),
                intake.scheduledAt(),
                intake.status(),
                intake.createdAt(),
                intake.confirmedAt(),
                intake.confirmationChannel(),
                intake.unconfirmedReportedAt()));
      }
    }
    var cancelled =
        current.values().stream()
            .filter(Intake::isPending)
            .filter(
                intake -> !intake.scheduledAt().isBefore(from) && intake.scheduledAt().isBefore(to))
            .sorted(Comparator.comparing(Intake::scheduledAt).thenComparing(Intake::id))
            .toList();
    return new IntakeScheduleChangeSet(pending, cancelled);
  }
}
