package com.tata.identitysubscription.interfaces.rest.transform;

import com.tata.identitysubscription.domain.model.commands.RequestNewVerificationCommand;
import com.tata.identitysubscription.interfaces.rest.resources.CreateEmailVerificationRequestResource;

public final class CreateEmailVerificationRequestCommandFromResourceAssembler {
    private CreateEmailVerificationRequestCommandFromResourceAssembler() {}

    public static RequestNewVerificationCommand toCommandFromResource(
            CreateEmailVerificationRequestResource resource
    ) {
        return new RequestNewVerificationCommand(resource.email());
    }
}
