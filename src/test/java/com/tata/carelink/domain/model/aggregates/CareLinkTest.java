package com.tata.carelink.domain.model.aggregates;

import com.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.tata.carelink.domain.services.CareLinkConfirmationPolicy;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class CareLinkTest {
    private static final Instant NOW = Instant.parse("2026-10-05T12:00:00Z");

    @Test
    void validCodeAndConsentConfirmTheCareLink() {
        var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15));
        var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

        link.accept("TATA-4821", NOW.plusSeconds(30));
        link.registerConsent(true, NOW.plusSeconds(60));

        assertTrue(new CareLinkConfirmationPolicy().canConfirm(link, NOW.plusSeconds(60)));

        link.confirm(NOW.plusSeconds(60));

        assertEquals(CareLinkStatus.CONFIRMED, link.status());
        assertTrue(link.isActive());
        assertNotNull(link.confirmedAt());
    }

    @Test
    void expiredCodeCannotBeAccepted() {
        var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(1));
        var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

        assertThrows(
                IllegalStateException.class,
                () -> link.accept("TATA-4821", NOW.plus(Duration.ofMinutes(2)))
        );
        assertEquals(CareLinkStatus.PENDING, link.status());
    }

    @Test
    void codeCannotBeUsedTwice() {
        var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15));
        var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

        link.accept("TATA-4821", NOW.plusSeconds(10));

        assertThrows(
                IllegalStateException.class,
                () -> link.accept("TATA-4821", NOW.plusSeconds(20))
        );
    }

    @Test
    void rejectedConsentRevokesTheCareLink() {
        var code = LinkingCode.issue("TATA-4821", NOW, Duration.ofMinutes(15));
        var link = CareLink.createPending("caregiver-1", "adult-1", code, NOW);

        link.accept("TATA-4821", NOW.plusSeconds(10));
        link.registerConsent(false, NOW.plusSeconds(20));

        assertEquals(CareLinkStatus.REVOKED, link.status());
        assertFalse(link.isActive());
    }
}
