package com.tata.treatmentmanagement.interfaces.rest;

import com.tata.treatmentmanagement.application.commandservices.TreatmentCommandService;
import com.tata.treatmentmanagement.application.queryservices.TreatmentQueryService;
import com.tata.treatmentmanagement.domain.model.commands.DeactivateMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.RegisterMedicationCommand;
import com.tata.treatmentmanagement.domain.model.commands.UpdateMedicationCommand;
import com.tata.treatmentmanagement.interfaces.rest.resources.MedicationResources.*;
import com.tata.treatmentmanagement.interfaces.rest.transform.TreatmentResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MedicationsController {
    private final TreatmentCommandService commands;
    private final TreatmentQueryService queries;

    public MedicationsController(TreatmentCommandService commands, TreatmentQueryService queries) {
        this.commands = commands;
        this.queries = queries;
    }

    @PostMapping("/older-adults/{olderAdultId}/medications")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationResponse register(
            @PathVariable String olderAdultId,
            @Valid @RequestBody RegisterMedicationRequest resource
    ) {
        return TreatmentResourceAssembler.toResource(commands.registerMedication(
                new RegisterMedicationCommand(resource.caregiverId(), olderAdultId, resource.name(), resource.presentation())
        ));
    }

    @PutMapping("/medications/{medicationId}")
    public MedicationResponse update(
            @PathVariable String medicationId,
            @Valid @RequestBody UpdateMedicationRequest resource
    ) {
        return TreatmentResourceAssembler.toResource(commands.updateMedication(
                new UpdateMedicationCommand(resource.caregiverId(), medicationId, resource.name(), resource.presentation())
        ));
    }

    @PostMapping("/medications/{medicationId}/deactivation")
    public MedicationResponse deactivate(
            @PathVariable String medicationId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(
                commands.deactivateMedication(new DeactivateMedicationCommand(caregiverId, medicationId))
        );
    }

    @GetMapping("/medications/{medicationId}")
    public MedicationResponse detail(
            @PathVariable String medicationId,
            @RequestParam String caregiverId
    ) {
        return TreatmentResourceAssembler.toResource(queries.getMedication(caregiverId, medicationId));
    }
}
