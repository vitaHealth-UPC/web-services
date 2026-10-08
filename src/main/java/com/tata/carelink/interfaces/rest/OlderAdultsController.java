package com.tata.carelink.interfaces.rest;

import com.tata.carelink.application.commandservices.CareLinkCommandService;
import com.tata.carelink.application.queryservices.CareLinkQueryService;
import com.tata.carelink.interfaces.rest.resources.OlderAdultProfileResource;
import com.tata.carelink.interfaces.rest.resources.RegisterOlderAdultProfileResource;
import com.tata.carelink.interfaces.rest.transform.CareLinkResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/older-adults")
public class OlderAdultsController {
    private final CareLinkCommandService commandService;
    private final CareLinkQueryService queryService;

    public OlderAdultsController(
            CareLinkCommandService commandService,
            CareLinkQueryService queryService
    ) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OlderAdultProfileResource register(@Valid @RequestBody RegisterOlderAdultProfileResource resource) {
        return CareLinkResourceAssembler.toResource(
                commandService.registerOlderAdult(CareLinkResourceAssembler.toCommand(resource))
        );
    }

    @GetMapping("/{olderAdultId}")
    public OlderAdultProfileResource get(@PathVariable String olderAdultId) {
        return CareLinkResourceAssembler.toResource(queryService.getOlderAdult(olderAdultId));
    }
}
