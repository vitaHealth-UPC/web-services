package com.tata.intakeexecution.domain.model.aggregates;

import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class IntakeConfirmationTest {

    @Test
    void confirmsPendingIntakeExactlyOnce() {
        var intake = pendingIntake();

        assertTrue(intake.confirm());
        assertEquals(IntakeStatus.CONFIRMED, intake.status());

        assertFalse(intake.confirm());
        assertEquals(IntakeStatus.CONFIRMED, intake.status());
    }

    @Test
    void retryOnLateIntakeIsIdempotent() {
        var intake = Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                Instant.parse("2026-10-06T13:00:00Z"),
                IntakeStatus.LATE,
                Instant.parse("2026-10-05T12:00:00Z")
        );

        assertFalse(intake.confirm());
        assertEquals(IntakeStatus.LATE, intake.status());
    }

    @Test
    void omittedIntakeCannotBeImplicitlyReplacedByConfirmation() {
        var intake = Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                Instant.parse("2026-10-06T13:00:00Z"),
                IntakeStatus.OMITTED,
                Instant.parse("2026-10-05T12:00:00Z")
        );

        assertThrows(IllegalStateException.class, intake::confirm);
        assertEquals(IntakeStatus.OMITTED, intake.status());
    }

    private static Intake pendingIntake() {
        return Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                Instant.parse("2026-10-06T13:00:00Z"),
                IntakeStatus.PENDING,
                Instant.parse("2026-10-05T12:00:00Z")
        );
    }
}
