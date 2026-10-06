package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.commandservices.PinCommandService;
import com.tata.identitysubscription.domain.model.commands.RegisterPinCommand;
import com.tata.identitysubscription.interfaces.rest.resources.RegisterPinResource;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pin-credentials")
public class PinCredentialsController {
    private final PinCommandService service;

    public PinCredentialsController(PinCommandService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void register(@Valid @RequestBody RegisterPinResource resource) {
        service.register(new RegisterPinCommand(resource.olderAdultId(), resource.pin()));
    }
}
