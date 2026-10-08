package com.tata.familymonitoring.interfaces.rest;

import com.tata.familymonitoring.application.commandservices.CreateCaregiverNoteCommandService;
import com.tata.familymonitoring.application.queryservices.GetCaregiverNotesQueryService;
import com.tata.familymonitoring.domain.model.queries.GetCaregiverNotesQuery;
import com.tata.familymonitoring.interfaces.rest.resources.CaregiverNoteResource;
import com.tata.familymonitoring.interfaces.rest.resources.CreateCaregiverNoteResource;
import com.tata.familymonitoring.interfaces.rest.resources.ErrorResource;
import com.tata.familymonitoring.interfaces.rest.transform.CaregiverNoteResourceFromEntityAssembler;
import com.tata.familymonitoring.interfaces.rest.transform.CreateCaregiverNoteCommandFromResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/older-adults/{olderAdultId}/notes")
@Tag(name = "Caregiver Notes", description = "Notes a caregiver keeps about interventions")
public class CaregiverNotesController {

  private final CreateCaregiverNoteCommandService createNoteHandler;
  private final GetCaregiverNotesQueryService notesHandler;
  private final com.tata.familymonitoring.application.queryservices.CareRelationshipQueryService access;

  public CaregiverNotesController(
      CreateCaregiverNoteCommandService createNoteHandler,
      GetCaregiverNotesQueryService notesHandler,
      com.tata.familymonitoring.application.queryservices.CareRelationshipQueryService access) {
    this.createNoteHandler = createNoteHandler;
    this.notesHandler = notesHandler;
    this.access = access;
  }

  @Operation(
      summary = "Register a follow-up note",
      description = "Stores the note with its date and author (US-30).")
  @ApiResponse(responseCode = "201", description = "Note registered")
  @ApiResponse(responseCode = "400", description = "Invalid note",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @ApiResponse(responseCode = "404", description = "The older adult has no active follow-up",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public CaregiverNoteResource createNote(
      @PathVariable String olderAdultId, @Valid @RequestBody CreateCaregiverNoteResource resource) {
    access.check(resource.familiarId(), olderAdultId);
    return CaregiverNoteResourceFromEntityAssembler.toResourceFromEntity(
        createNoteHandler.handle(
            CreateCaregiverNoteCommandFromResourceAssembler.toCommandFromResource(
                olderAdultId, resource)));
  }

  @Operation(
      summary = "List the follow-up notes",
      description = "Registered notes, most recent first (US-30).")
  @ApiResponse(responseCode = "200", description = "Notes returned (possibly empty)")
  @ApiResponse(responseCode = "404", description = "The older adult has no active follow-up",
      content = @Content(schema = @Schema(implementation = ErrorResource.class)))
  @GetMapping
  public List<CaregiverNoteResource> getNotes(@PathVariable String olderAdultId,
      @org.springframework.web.bind.annotation.RequestParam String caregiverId) {
    access.check(caregiverId, olderAdultId);
    return notesHandler.handle(new GetCaregiverNotesQuery(olderAdultId)).stream()
        .map(CaregiverNoteResourceFromEntityAssembler::toResourceFromEntity)
        .toList();
  }
}
