package com.tata.intakeexecution;

import com.tata.intakeexecution.domain.model.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.commandservices.ConfirmIntakeCommandHandler;
import com.tata.intakeexecution.domain.model.aggregates.Intake;
import com.tata.intakeexecution.domain.model.events.IntakeConfirmed;
import com.tata.intakeexecution.domain.model.events.IntakeUnconfirmed;
import com.tata.intakeexecution.domain.model.valueobjects.ConfirmationChannel;
import com.tata.intakeexecution.domain.model.valueobjects.MedicationSnapshot;
import com.tata.intakeexecution.domain.repositories.IntakeRepository;
import com.tata.omissionescalation.domain.repositories.IOmissionCaseRepository;
import com.tata.omissionescalation.domain.model.valueobjects.OmissionCaseStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.*;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {"spring.datasource.url=jdbc:h2:mem:confirmation;DB_CLOSE_DELAY=-1", "tata.omission.grace-period=PT30M"})
@ActiveProfiles("test")
class IntakeConfirmationIntegrationTest {
    @Autowired IntakeRepository intakes;
    @Autowired IOmissionCaseRepository cases;
    @Autowired ConfirmIntakeCommandHandler handler;
    @Autowired ApplicationEventPublisher publisher;
    @Autowired EventRecorder recorder;

    @TestConfiguration
    static class Configuration {
        @Bean EventRecorder eventRecorder() { return new EventRecorder(); }
    }
    static class EventRecorder {
        final List<IntakeConfirmed> events = new CopyOnWriteArrayList<>();
        @EventListener public void confirmed(IntakeConfirmed event) { events.add(event); }
    }

    private Intake pending() {
        var intake = Intake.createScheduled(java.util.UUID.randomUUID().toString(), java.util.UUID.randomUUID().toString(),
                java.util.UUID.randomUUID().toString(), new MedicationSnapshot("Losartan", "1 tablet", "With water"),
                Instant.now(), Instant.now());
        return intakes.saveAll(List.of(intake)).getFirst();
    }

    @Test void confirmationResolvesUuidOmissionAndPersistsOriginalMetadata() {
        var intake = pending();
        publisher.publishEvent(new IntakeUnconfirmed(intake.id(), intake.olderAdultId(), "Losartan", intake.scheduledAt()));
        handler.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.TOUCH));
        var saved = intakes.findById(intake.id()).orElseThrow();
        assertNotNull(saved.confirmedAt());
        assertEquals(ConfirmationChannel.TOUCH, saved.confirmationChannel());
        assertEquals(OmissionCaseStatus.RESOLVED, cases.findByIntakeId(intake.id()).orElseThrow().getStatus());
        handler.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.VOICE));
        var retried = intakes.findById(intake.id()).orElseThrow();
        assertEquals(saved.confirmedAt(), retried.confirmedAt());
        assertEquals(ConfirmationChannel.TOUCH, retried.confirmationChannel());
        assertEquals(1, recorder.events.stream().filter(e -> e.intakeId().equals(intake.id())).count());
    }

    @Test void concurrentTouchAndVoicePublishOneConfirmation() throws Exception {
        var intake = pending();
        var start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var touch = executor.submit(() -> { start.await(); return handler.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.TOUCH)); });
            var voice = executor.submit(() -> { start.await(); return handler.handle(new ConfirmIntakeCommand(intake.id(), ConfirmationChannel.VOICE)); });
            start.countDown();
            assertNotNull(touch.get(20, TimeUnit.SECONDS));
            assertNotNull(voice.get(20, TimeUnit.SECONDS));
        }
        assertEquals(1, recorder.events.stream().filter(e -> e.intakeId().equals(intake.id())).count());
    }
}
