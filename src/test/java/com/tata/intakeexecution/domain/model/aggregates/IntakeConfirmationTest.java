package com.tata.intakeexecution.domain.model.aggregates;

import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.IntakeStatus;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class IntakeConfirmationTest {

    private static final Instant SCHEDULED_AT = Instant.parse("2026-10-06T13:00:00Z");

    @Test
    void confirmsPendingIntakeExactlyOnce() {
        var intake = pendingIntake();

        assertTrue(intake.confirm(SCHEDULED_AT, ConfirmationChannel.TOUCH));
        assertEquals(IntakeStatus.CONFIRMED, intake.status());

        assertFalse(intake.confirm(SCHEDULED_AT.plusSeconds(30), ConfirmationChannel.VOICE));
        assertEquals(IntakeStatus.CONFIRMED, intake.status());
        assertEquals(ConfirmationChannel.TOUCH, intake.confirmationChannel());
    }

    @Test
    void confirmationAfterScheduledTimeIsLate() {
        var intake = pendingIntake();

        assertTrue(intake.confirm(SCHEDULED_AT.plusSeconds(60), ConfirmationChannel.VOICE));

        assertEquals(IntakeStatus.LATE, intake.status());
        assertEquals(SCHEDULED_AT.plusSeconds(60), intake.confirmedAt());
        assertEquals(ConfirmationChannel.VOICE, intake.confirmationChannel());
    }

    @Test
    void retryOnLateIntakeIsIdempotent() {
        var intake = pendingIntake();
        intake.confirm(SCHEDULED_AT.plusSeconds(60), ConfirmationChannel.TOUCH);

        assertFalse(intake.confirm(SCHEDULED_AT.plusSeconds(120), ConfirmationChannel.VOICE));
        assertEquals(IntakeStatus.LATE, intake.status());
        assertEquals(ConfirmationChannel.TOUCH, intake.confirmationChannel());
    }

    @Test
    void omittedIntakeCannotBeImplicitlyReplacedByConfirmation() {
        var intake = Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                SCHEDULED_AT,
                IntakeStatus.OMITTED,
                Instant.parse("2026-10-05T12:00:00Z")
        );

        assertThrows(
                IllegalStateException.class,
                () -> intake.confirm(SCHEDULED_AT.plusSeconds(3600), ConfirmationChannel.VOICE)
        );
        assertEquals(IntakeStatus.OMITTED, intake.status());
    }

    @Test
    void definitiveOmissionCanSupersedeLateOutcomeButNotOnTimeConfirmation() {
        var late = pendingIntake();
        late.confirm(SCHEDULED_AT.plusSeconds(60), ConfirmationChannel.TOUCH);
        assertTrue(late.markOmitted());
        assertEquals(IntakeStatus.OMITTED, late.status());

        var onTime = pendingIntake();
        onTime.confirm(SCHEDULED_AT, ConfirmationChannel.TOUCH);
        assertFalse(onTime.markOmitted());
        assertEquals(IntakeStatus.CONFIRMED, onTime.status());
    }

    private static Intake pendingIntake() {
        return Intake.rehydrate(
                "intake-1",
                "treatment-1",
                "medication-1",
                "adult-1",
                new MedicationSnapshot("Losartán 50 mg", "1 comprimido", "Con agua"),
                SCHEDULED_AT,
                IntakeStatus.PENDING,
                Instant.parse("2026-10-05T12:00:00Z")
        );
    }
}