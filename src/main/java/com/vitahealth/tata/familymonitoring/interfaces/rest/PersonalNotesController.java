package com.vitahealth.tata.familymonitoring.interfaces.rest;

import com.vitahealth.tata.familymonitoring.application.commandservices.PersonalNoteCommandService;
import com.vitahealth.tata.familymonitoring.application.queryservices.PersonalNoteQueryService;
import com.vitahealth.tata.familymonitoring.domain.model.commands.CreatePersonalNoteCommand;
import com.vitahealth.tata.familymonitoring.domain.model.queries.GetPersonalNotesQuery;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.CreatePersonalNoteResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.PersonalNoteResource;
import com.vitahealth.tata.familymonitoring.interfaces.rest.transform.PersonalNoteResourceAssembler;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** Exposes private reminders only to the authenticated older adult owner. */
@RestController
@RequestMapping("/api/v1/me/notes")
@Tag(name = "Personal Notes")
public class PersonalNotesController {
  private final PersonalNoteCommandService commands;
  private final PersonalNoteQueryService queries;

  /** Injects separate private reminder write and read boundaries. */
  public PersonalNotesController(
      PersonalNoteCommandService commands, PersonalNoteQueryService queries) {
    this.commands = commands;
    this.queries = queries;
  }

  /** Reads only the adult identity carried by the authenticated session. */
  @GetMapping
  @Operation(summary = "List my private routine notes")
  public List<PersonalNoteResource> list(
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session) {
    return queries.handle(new GetPersonalNotesQuery(owner(session))).stream()
        .map(PersonalNoteResourceAssembler::toResource)
        .toList();
  }

  /** Creates private content without accepting a client-controlled owner. */
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(
      summary = "Add a private routine note",
      description =
          "The owner comes from the older adult PIN session. Caregiver follow-up notes remain"
              + " separate.")
  public PersonalNoteResource create(
      @AuthenticationPrincipal(expression = "session()") AuthenticatedSession session,
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
