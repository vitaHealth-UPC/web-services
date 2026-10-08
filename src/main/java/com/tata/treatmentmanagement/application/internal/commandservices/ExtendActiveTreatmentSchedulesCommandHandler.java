package com.tata.treatmentmanagement.application.internal.commandservices;

import com.tata.treatmentmanagement.application.events.TreatmentScheduleChangedEvent;
import com.tata.treatmentmanagement.application.events.TreatmentScheduleEventPublisher;
import com.tata.treatmentmanagement.domain.model.aggregates.Medication;
import com.tata.treatmentmanagement.domain.model.aggregates.Treatment;
import com.tata.treatmentmanagement.domain.model.valueobjects.TreatmentStatus;
import com.tata.treatmentmanagement.domain.repositories.MedicationRepository;
import com.tata.treatmentmanagement.domain.repositories.TreatmentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Republishes schedule-change events for every active treatment so Intake Execution can extend
 * its rolling generation horizon without mutating resolved intake history.
 */
@Service
@Transactional
public class ExtendActiveTreatmentSchedulesCommandHandler {
    private final TreatmentRepository treatments;
    private final MedicationRepository medications;
    private final TreatmentScheduleEventPublisher scheduleEventPublisher;

    public ExtendActiveTreatmentSchedulesCommandHandler(
            TreatmentRepository treatments,
            MedicationRepository medications,
            TreatmentScheduleEventPublisher scheduleEventPublisher
    ) {
        this.treatments = treatments;
        this.medications = medications;
        this.scheduleEventPublisher = scheduleEventPublisher;
    }

    public int handle() {
        int published = 0;
        for (Treatment treatment : treatments.findByStatus(TreatmentStatus.ACTIVE)) {
            if (publishSchedule(treatment)) {
                published++;
            }
        }
        return published;
    }

    private boolean publishSchedule(Treatment treatment) {
        var regimen = treatment.regimen();
        if (regimen == null) {
            return false;
        }
        Medication medication = medications.findById(regimen.medicationId()).orElse(null);
        if (medication == null || !medication.active()) {
            return false;
        }
        scheduleEventPublisher.publish(new TreatmentScheduleChangedEvent(
                treatment.id(),
                regimen.medicationId(),
                treatment.olderAdultId(),
                medication.name(),
                regimen.dose(),
                regimen.frequency(),
                regimen.scheduledTimes(),
                regimen.instructions(),
                regimen.reminderLeadMinutes(),
                true
        ));
        return true;
    }
}
