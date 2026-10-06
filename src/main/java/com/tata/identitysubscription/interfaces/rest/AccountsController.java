package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.tata.identitysubscription.interfaces.rest.resources.AccountResource;
import com.tata.identitysubscription.interfaces.rest.resources.RegisterAccountResource;
import com.tata.identitysubscription.interfaces.rest.resources.VerifyEmailResource;
import com.tata.identitysubscription.interfaces.rest.transform.IdentityResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountsController {
    private final AccountCommandService service;

    public AccountsController(AccountCommandService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResource register(@Valid @RequestBody RegisterAccountResource resource) {
        return IdentityResourceAssembler.toResource(
                service.register(IdentityResourceAssembler.toCommand(resource))
        );
    }

    @PostMapping("/verification")
    public AccountResource verify(@Valid @RequestBody VerifyEmailResource resource) {
        return IdentityResourceAssembler.toResource(
                service.verify(IdentityResourceAssembler.toCommand(resource))
        );
    }
}
