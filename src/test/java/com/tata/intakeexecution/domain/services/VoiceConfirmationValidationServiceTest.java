package com.tata.intakeexecution.domain.services;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VoiceConfirmationValidationServiceTest {

    private final VoiceConfirmationValidationService service =
            new VoiceConfirmationValidationService();

    @Test
    void acceptsHighConfidenceExplicitMedicationConfirmation() {
        assertTrue(service.isValidConfirmation(
                "Confirmo que tomé Losartán cincuenta miligramos.",
                0.94,
                "Losartán 50 mg"
        ));
    }

    @Test
    void acceptsGenericMedicationConfirmation() {
        assertTrue(service.isValidConfirmation(
                "Ya tomé mi medicamento.",
                0.88,
                "Losartán 50 mg"
        ));
    }

    @Test
    void rejectsLowConfidenceRecognition() {
        assertFalse(service.isValidConfirmation(
                "Confirmo que tomé Losartán.",
                0.42,
                "Losartán 50 mg"
        ));
    }

    @Test
    void rejectsAmbiguousAffirmation() {
        assertFalse(service.isValidConfirmation(
                "Sí.",
                0.99,
                "Losartán 50 mg"
        ));
    }

    @Test
    void rejectsPhraseForAnotherAction() {
        assertFalse(service.isValidConfirmation(
                "Recuérdame tomar Losartán más tarde.",
                0.97,
                "Losartán 50 mg"
        ));
    }
    @Test
    void rejectsNegatedAndUncertainConfirmations() {
        for (String phrase : new String[]{"No he tomado mi medicamento", "Confirmo que no tomé Losartán", "Todavía no tomé mi pastilla", "Quizás ya tomé mi medicamento"}) {
            assertFalse(service.isValidConfirmation(phrase, 0.99, "Losartán 50 mg"), phrase);
        }
    }

    @Test
    void requiresWholeWordsAndFiniteConfidence() {
        assertFalse(service.isValidConfirmation("Desconfirmo mi medicamento", 0.99, "Losartán"));
        assertFalse(service.isValidConfirmation("Confirmo losartanina", 0.99, "Losartán"));
        assertFalse(service.isValidConfirmation("Ya tomé mi medicamento", Double.NaN, "Losartán"));
        assertFalse(service.isValidConfirmation("Ya tomé mi medicamento", Double.POSITIVE_INFINITY, "Losartán"));
        assertFalse(service.isValidConfirmation("Ya tomé mi medicamento", 1.1, "Losartán"));
    }
}
