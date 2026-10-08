package com.tata.familymonitoring.interfaces.rest;

import com.tata.familymonitoring.application.commandservices.PersonalNoteCommandService;
import com.tata.familymonitoring.application.queryservices.PersonalNoteQueryService;
import com.tata.familymonitoring.domain.model.commands.CreatePersonalNoteCommand;
import com.tata.familymonitoring.interfaces.rest.resources.CreatePersonalNoteResource;
import com.tata.familymonitoring.interfaces.rest.resources.PersonalNoteResource;
import com.tata.familymonitoring.interfaces.rest.transform.PersonalNoteResourceAssembler;
import com.tata.identitysubscription.application.models.AuthenticatedSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/v1/me/notes")
@Tag(name = "Personal Notes")
public class PersonalNotesController {
  private final PersonalNoteCommandService commands;
  private final PersonalNoteQueryService queries;

  public PersonalNotesController(
      PersonalNoteCommandService commands, PersonalNoteQueryService queries) {
    this.commands = commands;
    this.queries = queries;
  }

  @GetMapping
  @Operation(summary = "List my private routine notes")
  public List<PersonalNoteResource> list(@AuthenticationPrincipal AuthenticatedSession session) {
    return queries.list(owner(session)).stream()
        .map(PersonalNoteResourceAssembler::toResource)
        .toList();
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Add a private routine note",
      description =
          "The owner comes from the older adult PIN session. Caregiver follow-up notes remain"
              + " separate.")
  public PersonalNoteResource create(
      @AuthenticationPrincipal AuthenticatedSession session,
      @Valid @RequestBody CreatePersonalNoteResource resource) {
    return PersonalNoteResourceAssembler.toResource(
        commands.handle(
            new CreatePersonalNoteCommand(
                owner(session), resource.title(), resource.text(), resource.category())));
  }

  private String owner(AuthenticatedSession session) {
    if (session == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
    if (session.role() != AuthenticatedSession.Role.OLDER_ADULT)
      throw new ResponseStatusException(HttpStatus.FORBIDDEN);
    return session.subjectId();
  }
}
