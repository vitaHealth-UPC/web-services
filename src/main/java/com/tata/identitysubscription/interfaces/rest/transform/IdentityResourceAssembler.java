package com.tata.identitysubscription.interfaces.rest.transform;

import com.tata.identitysubscription.application.models.AccountResult;
import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.tata.identitysubscription.interfaces.rest.resources.*;

public final class IdentityResourceAssembler {
    private IdentityResourceAssembler() {}

    public static RegisterFamilyAccountCommand toCommand(RegisterAccountResource resource) {
        return new RegisterFamilyAccountCommand(resource.name(), resource.email(), resource.password());
    }

    public static VerifyEmailCommand toCommand(VerifyEmailResource resource) {
        return new VerifyEmailCommand(resource.email(), resource.code());
    }

    public static AuthenticateFamilyCommand toCommand(SignInResource resource) {
        return new AuthenticateFamilyCommand(resource.email(), resource.password());
    }

    public static AccountResource toResource(com.tata.identitysubscription.application.models.VerifiedAccountResult result) {
        var account=result.account(); var session=result.session();
        return new AccountResource(account.id(), account.name(), account.email(), account.status().name(), session.accessToken(), session.expiresAt());
    }
    public static AccountResource toResource(AccountResult result) {
        return new AccountResource(result.id(), result.name(), result.email(), result.status().name());
    }

    public static AuthenticatedAccountResource toResource(SessionResult result) {
        return new AuthenticatedAccountResource(result.accountId(), result.accessToken(), result.expiresAt());
    }
}
