package com.tata.accessibilitypreferences.infrastructure.events;

import com.tata.accessibilitypreferences.interfaces.events.AccountEnabledEventConsumer;
import com.tata.identitysubscription.domain.model.events.AccountEnabled;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AccountEnabledEventListener {
    private final AccountEnabledEventConsumer consumer;

    public AccountEnabledEventListener(AccountEnabledEventConsumer consumer) {
        this.consumer = consumer;
    }

    @EventListener
    public void on(AccountEnabled event) {
        consumer.consume(event);
    }
}
