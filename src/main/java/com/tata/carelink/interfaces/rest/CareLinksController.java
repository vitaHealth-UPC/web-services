package com.tata.carelink.interfaces.rest;

import com.tata.carelink.application.commandservices.CareLinkCommandService;
import com.tata.carelink.application.queryservices.CareLinkQueryService;
import com.tata.carelink.interfaces.rest.resources.*;
import com.tata.carelink.interfaces.rest.transform.CareLinkResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/care-links")
public class CareLinksController {
    private final CareLinkCommandService commandService;
    private final CareLinkQueryService queryService;

    public CareLinksController(
            CareLinkCommandService commandService,
            CareLinkQueryService queryService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping("/linking-codes")
    @ResponseStatus(HttpStatus.CREATED)
    public CareLinkResource generateCode(@Valid @RequestBody GenerateLinkingCodeResource resource) {
        return CareLinkResourceAssembler.toResource(
                commandService.generateLinkingCode(CareLinkResourceAssembler.toCommand(resource))
        );
    }

    @PostMapping("/acceptances")
    public CareLinkResource accept(@Valid @RequestBody AcceptCareLinkResource resource) {
        return CareLinkResourceAssembler.toResource(
                commandService.accept(CareLinkResourceAssembler.toCommand(resource))
        );
    }

    @PostMapping("/{careLinkId}/consent")
    public CareLinkResource registerConsent(
            @PathVariable String careLinkId,
            @RequestBody RegisterConsentResource resource
    ) {
        return CareLinkResourceAssembler.toResource(
                commandService.registerConsent(CareLinkResourceAssembler.toCommand(careLinkId, resource))
        );
    }

    @GetMapping("/{careLinkId}")
    public CareLinkResource get(@PathVariable String careLinkId) {
        return CareLinkResourceAssembler.toResource(queryService.getById(careLinkId));
    }

    public record ConfirmedCareLinkResource(String id, String olderAdultId, String olderAdultName,
            java.time.Instant confirmedAt) {}

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "List confirmed care links of a caregiver",
            description = "Returns active links with consent, newest first. An empty list means no confirmed links.")
    public java.util.List<ConfirmedCareLinkResource> list(@RequestParam String caregiverId) {
        return queryService.getConfirmedByCaregiver(caregiverId).stream()
                .map(link -> new ConfirmedCareLinkResource(link.id(), link.olderAdultId(),
                        queryService.getOlderAdult(link.olderAdultId()).fullName(), link.confirmedAt())).toList();
    }

    @GetMapping("/authorization")
    public boolean isAuthorized(
            @RequestParam String caregiverId,
            @RequestParam String olderAdultId
    ) {
        return queryService.isAuthorized(caregiverId, olderAdultId);
    }
}
