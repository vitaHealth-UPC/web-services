package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.tata.identitysubscription.interfaces.rest.resources.AccountResource;
import com.tata.identitysubscription.interfaces.rest.resources.CreateEmailVerificationRequestResource;
import com.tata.identitysubscription.interfaces.rest.transform.CreateEmailVerificationRequestCommandFromResourceAssembler;
import com.tata.identitysubscription.interfaces.rest.transform.IdentityResourceAssembler;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email-verification-requests")
public class EmailVerificationRequestsController {
    private final AccountCommandService service;

    public EmailVerificationRequestsController(AccountCommandService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    public AccountResource create(@Valid @RequestBody CreateEmailVerificationRequestResource resource) {
        return IdentityResourceAssembler.toResource(
                service.requestNewVerification(
                        CreateEmailVerificationRequestCommandFromResourceAssembler.toCommandFromResource(resource)
                )
        );
    }
}
