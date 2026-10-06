package com.tata.intakeexecution.interfaces.rest;

import com.tata.intakeexecution.application.commands.ConfirmIntakeByVoiceCommand;
import com.tata.intakeexecution.application.commands.ConfirmIntakeCommand;
import com.tata.intakeexecution.application.internal.commandservices.ConfirmIntakeByVoiceCommandHandler;
import com.tata.intakeexecution.application.internal.commandservices.ConfirmIntakeCommandHandler;
import com.tata.intakeexecution.application.internal.queryservices.GetIntakeAgendaQueryHandler;
import com.tata.intakeexecution.application.internal.queryservices.GetIntakeDetailQueryHandler;
import com.tata.intakeexecution.application.internal.queryservices.GetNextIntakeQueryHandler;
import com.tata.intakeexecution.application.models.IntakeResult;
import com.tata.intakeexecution.application.models.VoiceConfirmationResult;
import com.tata.intakeexecution.interfaces.rest.resources.ConfirmIntakeResource;
import com.tata.intakeexecution.interfaces.rest.resources.IntakeResource;
import com.tata.intakeexecution.interfaces.rest.resources.VoiceConfirmationResource;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
public class IntakesController {

    private static final long MAX_VOICE_AUDIO_BYTES = 5L * 1024L * 1024L;

    private final GetNextIntakeQueryHandler getNextIntake;
    private final GetIntakeDetailQueryHandler getIntakeDetail;
    private final ConfirmIntakeCommandHandler confirmIntake;
    private final ConfirmIntakeByVoiceCommandHandler confirmIntakeByVoice;
    private final GetIntakeAgendaQueryHandler getAgenda;

    public IntakesController(
            GetNextIntakeQueryHandler getNextIntake,
            GetIntakeDetailQueryHandler getIntakeDetail,
            ConfirmIntakeCommandHandler confirmIntake,
            ConfirmIntakeByVoiceCommandHandler confirmIntakeByVoice,
            GetIntakeAgendaQueryHandler getAgenda
    ) {
        this.getNextIntake = getNextIntake;
        this.getIntakeDetail = getIntakeDetail;
        this.confirmIntake = confirmIntake;
        this.confirmIntakeByVoice = confirmIntakeByVoice;
        this.getAgenda = getAgenda;
    }

    @GetMapping("/older-adults/{olderAdultId}/intakes/agenda")
    public List<IntakeResource> agenda(
            @PathVariable String olderAdultId,
            @RequestParam Instant from,
            @RequestParam Instant to
    ) {
        return getAgenda.handle(olderAdultId, from, to).stream().map(this::toResource).toList();
    }

    @GetMapping("/older-adults/{olderAdultId}/intakes/next")
    public ResponseEntity<IntakeResource> next(@PathVariable String olderAdultId) {
        return getNextIntake.handle(olderAdultId)
                .map(this::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/intakes/{intakeId}")
    public ResponseEntity<IntakeResource> detail(@PathVariable String intakeId) {
        return getIntakeDetail.handle(intakeId)
                .map(this::toResource)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/intakes/{intakeId}/confirmation")
    public IntakeResource confirm(
            @PathVariable String intakeId,
            @Valid @RequestBody ConfirmIntakeResource resource
    ) {
        return toResource(confirmIntake.handle(
                new ConfirmIntakeCommand(intakeId, resource.channel())
        ));
    }

    @Operation(
            summary = "Recognize and validate a spoken intake confirmation",
            description = "Processes audio through the configured Speech-to-Text integration. "
                    + "Only a recognized and validated confirmation can change the intake state."
    )
    @PostMapping(
            value = "/intakes/{intakeId}/voice-confirmation",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public VoiceConfirmationResource confirmByVoice(
            @PathVariable String intakeId,
            @RequestPart("audio") MultipartFile audio,
            @RequestParam(defaultValue = "es-419") String language
    ) {
        if (audio.isEmpty()) {
            throw new IllegalArgumentException("audio is required");
        }
        if (audio.getSize() > MAX_VOICE_AUDIO_BYTES) {
            throw new IllegalArgumentException("audio exceeds the 5 MB limit");
        }

        String contentType = audio.getContentType() == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : audio.getContentType();

        final byte[] audioBytes;
        try {
            audioBytes = audio.getBytes();
        } catch (IOException exception) {
            throw new IllegalArgumentException("audio could not be read", exception);
        }

        return toResource(confirmIntakeByVoice.handle(
                new ConfirmIntakeByVoiceCommand(
                        intakeId,
                        audioBytes,
                        contentType,
                        language
                )
        ));
    }

    private VoiceConfirmationResource toResource(VoiceConfirmationResult result) {
        return new VoiceConfirmationResource(
                result.status(),
                result.transcript(),
                result.confidence(),
                result.intake() == null ? null : toResource(result.intake())
        );
    }

    private IntakeResource toResource(IntakeResult result) {
        return new IntakeResource(
                result.id(),
                result.treatmentId(),
                result.medicationId(),
                result.olderAdultId(),
                result.medicationName(),
                result.dose(),
                result.instructions(),
                result.scheduledAt(),
                result.status(),
                result.confirmedAt(),
                result.confirmationChannel()
        );
    }
}
