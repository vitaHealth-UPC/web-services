package com.tata.identitysubscription.application.commandservices;

import com.tata.identitysubscription.application.models.SubscriptionResult;
import com.tata.identitysubscription.domain.model.commands.ChangeSubscriptionCommand;

public interface ChangeSubscriptionCommandService {
    SubscriptionResult handle(ChangeSubscriptionCommand command);
}
