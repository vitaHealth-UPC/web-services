package com.tata.identitysubscription.application.commandservices;

import com.tata.identitysubscription.application.models.AccountResult;
import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;

public interface AccountCommandService {
    AccountResult register(RegisterFamilyAccountCommand command);
    AccountResult verify(VerifyEmailCommand command);
    SessionResult authenticate(AuthenticateFamilyCommand command);
}
