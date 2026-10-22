package com.vitahealth.tata.intakeexecution.interfaces.rest;

import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeByVoiceCommandService;
import com.vitahealth.tata.intakeexecution.application.commandservices.ConfirmIntakeCommandService;
import com.vitahealth.tata.intakeexecution.application.queryservices.GetIntakeAgendaQueryService;
import com.vitahealth.tata.intakeexecution.application.queryservices.GetIntakeDetailQueryService;
import com.vitahealth.tata.intakeexecution.application.queryservices.GetNextIntakeQueryService;
import com.vitahealth.tata.intakeexecution.application.validation.VoiceAudioRequirements;
import com.vitahealth.tata.intakeexecution.domain.model.commands.ConfirmIntakeByVoiceCommand;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeAgendaQuery;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetIntakeDetailQuery;
import com.vitahealth.tata.intakeexecution.domain.model.queries.GetNextIntakeQuery;
import com.vitahealth.tata.intakeexecution.interfaces.rest.resources.ConfirmIntakeResource;
import com.vitahealth.tata.intakeexecution.interfaces.rest.resources.IntakeResource;
import com.vitahealth.tata.intakeexecution.interfaces.rest.resources.VoiceConfirmationResource;
import com.vitahealth.tata.intakeexecution.interfaces.rest.transform.ConfirmIntakeCommandFromResourceAssembler;
import com.vitahealth.tata.intakeexecution.interfaces.rest.transform.IntakeResourceFromResultAssembler;
import com.vitahealth.tata.intakeexecution.interfaces.rest.transform.IntakeResponseAssembler;
import com.vitahealth.tata.intakeexecution.interfaces.rest.transform.VoiceConfirmationResourceFromResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Exposes intake agenda, evidence reads and explicit confirmation operations. */
@RestController
@RequestMapping("/api/v1")
public class IntakesController {

  private final GetNextIntakeQueryService getNextIntake;
  private final GetIntakeDetailQueryService getIntakeDetail;
  private final ConfirmIntakeCommandService confirmIntake;
  private final ConfirmIntakeByVoiceCommandService confirmIntakeByVoice;
  private final GetIntakeAgendaQueryService getAgenda;

  /** Injects public query and command contracts. */
  public IntakesController(
      GetNextIntakeQueryService getNextIntake,
      GetIntakeDetailQueryService getIntakeDetail,
      ConfirmIntakeCommandService confirmIntake,
      ConfirmIntakeByVoiceCommandService confirmIntakeByVoice,
      GetIntakeAgendaQueryService getAgenda) {
    this.getNextIntake = getNextIntake;
    this.getIntakeDetail = getIntakeDetail;
    this.confirmIntake = confirmIntake;
    this.confirmIntakeByVoice = confirmIntakeByVoice;
    this.getAgenda = getAgenda;
  }

  /** Returns ordered agenda evidence or a successful empty array. */
  @ApiResponse(
      responseCode = "200",
      description = "Ordered intake evidence",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              array = @ArraySchema(schema = @Schema(implementation = IntakeResource.class))))
  @GetMapping("/older-adults/{olderAdultId}/intakes/agenda")
  public ResponseEntity<?> agenda(
      @PathVariable String olderAdultId, @RequestParam Instant from, @RequestParam Instant to) {
    return IntakeResponseAssembler.toResponse(
        getAgenda.handle(new GetIntakeAgendaQuery(olderAdultId, from, to)),
        evidence ->
            ResponseEntity.ok(
                evidence.stream().map(IntakeResourceFromResultAssembler::toResource).toList()));
  }

  /** Returns the next pending intake, or 204 when no pending evidence exists. */
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Next pending intake",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = IntakeResource.class))),
    @ApiResponse(responseCode = "204", description = "No pending intake", content = @Content)
  })
  @GetMapping("/older-adults/{olderAdultId}/intakes/next")
  public ResponseEntity<?> next(@PathVariable String olderAdultId) {
    return IntakeResponseAssembler.toResponse(
        getNextIntake.handle(new GetNextIntakeQuery(olderAdultId)),
        evidence ->
            evidence
                .<ResponseEntity<?>>map(
                    value -> ResponseEntity.ok(IntakeResourceFromResultAssembler.toResource(value)))
                .orElseGet(() -> ResponseEntity.noContent().build()));
  }

  /** Returns stored evidence, or 404 for a valid missing identifier. */
  @ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "Stored intake evidence",
        content =
            @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = IntakeResource.class))),
    @ApiResponse(responseCode = "404", description = "Intake does not exist", content = @Content)
  })
  @GetMapping("/intakes/{intakeId}")
  public ResponseEntity<?> detail(@PathVariable String intakeId) {
    return IntakeResponseAssembler.toResponse(
        getIntakeDetail.handle(new GetIntakeDetailQuery(intakeId)),
        evidence ->
            evidence
                .<ResponseEntity<?>>map(
                    value -> ResponseEntity.ok(IntakeResourceFromResultAssembler.toResource(value)))
                .orElseGet(() -> ResponseEntity.notFound().build()));
  }

  /** Confirms the selected intake through the command contract. */
  @ApiResponse(
      responseCode = "200",
      description = "Confirmed intake or definitive replay",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = IntakeResource.class)))
  @PostMapping("/intakes/{intakeId}/confirmation")
  public ResponseEntity<?> confirm(
      @PathVariable String intakeId, @Valid @RequestBody ConfirmIntakeResource resource) {
    return IntakeResponseAssembler.toResponse(
        confirmIntake.handle(
            ConfirmIntakeCommandFromResourceAssembler.toCommand(intakeId, resource)),
        value -> ResponseEntity.ok(IntakeResourceFromResultAssembler.toResource(value)));
  }

  /** Reads bounded multipart audio for the voice-confirmation command. */
  @Operation(
      summary = "Recognize and validate a spoken intake confirmation",
      description =
          "Processes audio through the configured Speech-to-Text integration. "
              + "Only a recognized and validated confirmation can change the intake state.")
  @ApiResponse(
      responseCode = "200",
      description = "Recognition and confirmation outcome",
      content =
          @Content(
              mediaType = MediaType.APPLICATION_JSON_VALUE,
              schema = @Schema(implementation = VoiceConfirmationResource.class)))
  @PostMapping(
      value = "/intakes/{intakeId}/voice-confirmation",
      consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<?> confirmByVoice(
      @RequestHeader("Authorization") String authorization,
      @PathVariable String intakeId,
      @RequestPart("audio") MultipartFile audio,
      @Parameter(
              description = "Supported voice locale",
              schema = @Schema(allowableValues = {"es-419", "es-PE", "es-ES", "en-US"}))
          @RequestParam(defaultValue = "es-419")
          String language) {
    if (audio.isEmpty()) {
      throw new IllegalArgumentException("audio is required");
    }
    if (audio.getSize() > VoiceAudioRequirements.MAX_BYTES) {
      throw new IllegalArgumentException("audio exceeds the 5 MB limit");
    }

    String contentType =
        audio.getContentType() == null
            ? MediaType.APPLICATION_OCTET_STREAM_VALUE
            : audio.getContentType();

    final byte[] audioBytes;
    try {
      audioBytes = audio.getBytes();
    } catch (IOException exception) {
      throw new IllegalArgumentException("audio could not be read", exception);
    }

    return IntakeResponseAssembler.toResponse(
        confirmIntakeByVoice.handleAuthenticated(
            new ConfirmIntakeByVoiceCommand(intakeId, audioBytes, contentType, language),
            authorization.startsWith("Bearer ") ? authorization.substring(7) : ""),
        value -> ResponseEntity.ok(VoiceConfirmationResourceFromResultAssembler.toResource(value)));
  }
}
