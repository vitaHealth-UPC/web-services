package com.tata.accessibilitypreferences;

import com.tata.accessibilitypreferences.application.internal.commandservices.*;
import com.tata.accessibilitypreferences.domain.model.commands.UpdateQuietHoursCommand;
import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import com.tata.accessibilitypreferences.interfaces.rest.NotificationPreferencesController;
import com.tata.accessibilitypreferences.interfaces.rest.resources.*;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@ActiveProfiles("test")
class NotificationPreferencesAtomicityTest {
    @Autowired NotificationPreferencesController controller;
    @Autowired IUserPreferencesRepository repository;
    @MockitoSpyBean UpdateQuietHoursCommandHandler quietHours;
    @Test void failingSecondUpdateRollsBackChannelsAndDefaults() {
        var user = UUID.randomUUID().toString();
        doThrow(new IllegalStateException("simulated persistence failure")).when(quietHours).handle(any(UpdateQuietHoursCommand.class));
        assertThrows(IllegalStateException.class, () -> controller.update(user, new UpdateQuietHoursResource(null, List.of())));
        assertTrue(repository.findByUserId(user).isEmpty());
    }
}
