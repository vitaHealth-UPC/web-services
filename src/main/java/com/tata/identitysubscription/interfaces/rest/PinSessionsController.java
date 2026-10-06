package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.commandservices.PinCommandService;
import com.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.tata.identitysubscription.interfaces.rest.resources.PinSessionResource;
import com.tata.identitysubscription.interfaces.rest.resources.PinSignInResource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/pin-sessions")
public class PinSessionsController {
    private final PinCommandService service;

    public PinSessionsController(PinCommandService service) {
        this.service = service;
    }

    @PostMapping
    public PinSessionResource signIn(@Valid @RequestBody PinSignInResource resource) {
        var session = service.authenticate(new AuthenticateWithPinCommand(resource.olderAdultId(), resource.pin()));
        return new PinSessionResource(resource.olderAdultId(), session.accessToken(), session.expiresAt());
    }
}
