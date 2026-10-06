package com.tata.identitysubscription.application.commandservices;

import com.tata.identitysubscription.application.models.SessionResult;
import com.tata.identitysubscription.domain.model.commands.AuthenticateWithPinCommand;
import com.tata.identitysubscription.domain.model.commands.RegisterPinCommand;

public interface PinCommandService {
    void register(RegisterPinCommand command);
    SessionResult authenticate(AuthenticateWithPinCommand command);
}
