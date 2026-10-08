package com.tata.intakeexecution.infrastructure.persistence.jpa.assemblers;

import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.infrastructure.persistence.jpa.entities.IntakePersistenceEntity;

public final class IntakePersistenceAssembler {
    private IntakePersistenceAssembler() {}

    public static Intake toDomain(IntakePersistenceEntity entity) {
        return Intake.rehydrate(
                entity.getId(),
                entity.getTreatmentId(),
                entity.getMedicationId(),
                entity.getOlderAdultId(),
                new MedicationSnapshot(
                        entity.getMedicationName(),
                        entity.getDose(),
                        entity.getInstructions()
                ),
                entity.getScheduledAt(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getConfirmedAt(),
                entity.getConfirmationChannel(),
                entity.getUnconfirmedReportedAt()
        );
    }

    public static IntakePersistenceEntity toEntity(Intake intake) {
        var entity = new IntakePersistenceEntity(
                intake.id(),
                intake.treatmentId(),
                intake.medicationId(),
                intake.olderAdultId(),
                intake.medication().name(),
                intake.medication().dose(),
                intake.medication().instructions(),
                intake.scheduledAt(),
                intake.status(),
                intake.createdAt()
        );
        entity.setConfirmation(intake.confirmedAt(), intake.confirmationChannel());
        entity.setUnconfirmedReportedAt(intake.unconfirmedReportedAt());
        return entity;
    }
}
