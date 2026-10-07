package com.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.tata.accessibilitypreferences.domain.repositories.IUserPreferencesRepository;
import com.tata.identitysubscription.application.commandservices.AccountCommandService;
import com.tata.identitysubscription.application.internal.outboundservices.VerificationDeliveryPort;
import com.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.tata.identitysubscription.domain.model.events.AccountEnabled;
import com.tata.identitysubscription.domain.model.valueobjects.AccountStatus;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.event.EventListener;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifying the email enables the account and, through the event, creates its preferences. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AccountEnabledFlowTest {

    static final AtomicReference<String> LAST_CODE = new AtomicReference<>();
    static final List<AccountEnabled> ENABLED = new ArrayList<>();

    @TestConfiguration
    static class Capture {
        @Bean
        @Primary
        VerificationDeliveryPort capturingDelivery() {
            return (email, code) -> LAST_CODE.set(code);
        }

        @EventListener
        void on(AccountEnabled event) {
            ENABLED.add(event);
        }
    }

    @Autowired AccountCommandService accounts;
    @Autowired IUserPreferencesRepository preferences;

    @Test
    void verifyingTheEmailPublishesAccountEnabledAndCreatesTheDefaultPreferences() {
        ENABLED.clear();
        var email = "ana-" + UUID.randomUUID() + "@example.com";
        var registered = accounts.register(new RegisterFamilyAccountCommand("Ana", email, "S3cure-Password-123"));
        assertTrue(ENABLED.isEmpty());
        assertTrue(preferences.findByUserId(registered.id()).isEmpty());

        var verified = accounts.verify(new VerifyEmailCommand(email, LAST_CODE.get()));

        assertEquals(AccountStatus.ACTIVE, verified.status());
        assertEquals(1, ENABLED.size());
        assertEquals(registered.id(), ENABLED.getFirst().accountId());
        assertTrue(preferences.findByUserId(registered.id()).isPresent());
    }

    @Test
    void anInvalidCodeEnablesNothing() {
        ENABLED.clear();
        var email = "luis-" + UUID.randomUUID() + "@example.com";
        accounts.register(new RegisterFamilyAccountCommand("Luis", email, "S3cure-Password-123"));

        try {
            accounts.verify(new VerifyEmailCommand(email, "000000"));
        } catch (RuntimeException expected) {
            // the wrong code is rejected
        }

        assertTrue(ENABLED.isEmpty());
    }
}
