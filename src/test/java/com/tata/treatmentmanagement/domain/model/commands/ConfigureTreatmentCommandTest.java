package com.tata.treatmentmanagement.domain.model.commands;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ConfigureTreatmentCommandTest {
    @Test
    void callerCannotChangeTheCapturedSchedule() {
        var times = new ArrayList<>(List.of(LocalTime.of(8, 0)));
        var command = new ConfigureTreatmentCommand("caregiver", "treatment", "medication",
                "1 tablet", "daily", times, "", 5);
        times.clear();
        assertEquals(List.of(LocalTime.of(8, 0)), command.scheduledTimes());
        assertThrows(UnsupportedOperationException.class,
                () -> command.scheduledTimes().add(LocalTime.NOON));
    }
}
