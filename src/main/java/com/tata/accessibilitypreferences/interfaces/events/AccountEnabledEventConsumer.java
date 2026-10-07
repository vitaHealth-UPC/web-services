package com.tata.accessibilitypreferences.interfaces.events;

import com.tata.accessibilitypreferences.application.internal.eventhandlers.AccountEnabledEventHandler;
import com.tata.identitysubscription.domain.model.events.AccountEnabled;
import org.springframework.stereotype.Component;

@Component
public class AccountEnabledEventConsumer {
    private final AccountEnabledEventHandler eventHandler;

    public AccountEnabledEventConsumer(AccountEnabledEventHandler eventHandler) {
        this.eventHandler = eventHandler;
    }

    public void consume(AccountEnabled event) {
        eventHandler.handle(event);
    }
}
