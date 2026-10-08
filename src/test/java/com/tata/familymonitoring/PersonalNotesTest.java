package com.tata.familymonitoring;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tata.familymonitoring.application.internal.commandservices.PersonalNoteService;
import com.tata.familymonitoring.domain.model.commands.CreatePersonalNoteCommand;
import com.tata.familymonitoring.domain.model.entities.PersonalNote;
import com.tata.familymonitoring.domain.repositories.PersonalNoteRepository;
import com.tata.familymonitoring.interfaces.rest.PersonalNotesController;
import com.tata.familymonitoring.interfaces.rest.resources.CreatePersonalNoteResource;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import java.time.*;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PersonalNotesTest {
  private final PersonalNoteRepository repository = mock(PersonalNoteRepository.class);
  private final Instant now = Instant.parse("2026-10-08T13:00:00Z");
  private final PersonalNoteService service =
      new PersonalNoteService(repository, Clock.fixed(now, ZoneOffset.UTC));
  private final PersonalNotesController controller = new PersonalNotesController(service, service);

  @Test
  void ownerComesOnlyFromPinSessionAndServerSetsDate() {
    when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
    var result =
        controller.create(
            session(AuthenticatedSession.Role.OLDER_ADULT),
            new CreatePersonalNoteResource(
                " Agua ", " Preparar un vaso ", PersonalNote.Category.MEDICATION));
    assertEquals(now, result.recordedAt());
    assertEquals("Agua", result.title());
    verify(repository)
        .save(
            new PersonalNote(
                null,
                "adult-1",
                "Agua",
                "Preparar un vaso",
                PersonalNote.Category.MEDICATION,
                now));
  }

  @Test
  void caregiverAndSetupCannotReadOrWritePrivateNotes() {
    for (var role :
        List.of(AuthenticatedSession.Role.CAREGIVER, AuthenticatedSession.Role.LINK_SETUP)) {
      assertThrows(ResponseStatusException.class, () -> controller.list(session(role)));
      assertThrows(
          ResponseStatusException.class,
          () ->
              controller.create(
                  session(role),
                  new CreatePersonalNoteResource("a", "b", PersonalNote.Category.ROUTINE)));
    }
    verifyNoInteractions(repository);
  }

  @Test
  void unauthenticatedRequestsCannotRead() {
    assertThrows(ResponseStatusException.class, () -> controller.list(null));
    verifyNoInteractions(repository);
  }

  @Test
  void listsOnlyOwnNotes() {
    when(repository.findByOwner("adult-1")).thenReturn(List.of());
    assertTrue(controller.list(session(AuthenticatedSession.Role.OLDER_ADULT)).isEmpty());
    verify(repository).findByOwner("adult-1");
  }

  @Test
  void rejectsBlankAndOversizedNotes() {
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.handle(
                new CreatePersonalNoteCommand(
                    "adult-1", " ", "text", PersonalNote.Category.ROUTINE)));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            service.handle(
                new CreatePersonalNoteCommand(
                    "adult-1", "title", "x".repeat(1001), PersonalNote.Category.ROUTINE)));
    verifyNoInteractions(repository);
  }

  private AuthenticatedSession session(AuthenticatedSession.Role role) {
    return new AuthenticatedSession("adult-1", role, now.plusSeconds(60), null);
  }
}
