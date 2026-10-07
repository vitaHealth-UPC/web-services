package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.tata.identitysubscription.interfaces.rest.resources.AuthenticatedAccountResource;
import com.tata.identitysubscription.interfaces.rest.resources.SignInResource;
import com.tata.identitysubscription.interfaces.rest.transform.IdentityResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/sessions")
public class SessionsController {
    private final AccountCommandService service;

    public SessionsController(AccountCommandService service) {
        this.service = service;
    }

    @PostMapping
    public AuthenticatedAccountResource signIn(@Valid @RequestBody SignInResource resource) {
        return IdentityResourceAssembler.toResource(
                service.authenticate(IdentityResourceAssembler.toCommand(resource))
        );
    }
}
