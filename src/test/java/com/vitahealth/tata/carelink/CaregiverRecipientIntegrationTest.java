package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.carelink.domain.model.valueobjects.CareLinkStatus;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.entities.CareLinkPersistenceEntity;
import com.vitahealth.tata.carelink.infrastructure.persistence.jpa.repositories.CareLinkJpaRepository;
import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.ArrayList;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Verifies recipient selection uses bounded SQL and validated confirmed consent evidence. */
@SpringBootTest
@ActiveProfiles("test")
@Transactional
class CaregiverRecipientIntegrationTest {
  private static final Instant NOW = Instant.parse("2026-10-17T12:00:00Z");
  @Autowired CareLinkJpaRepository rows;
  @Autowired CareLinkContextFacade links;
  @Autowired EntityManager entities;

  private CareLinkPersistenceEntity relationship(
      String adult, CareLinkStatus status, boolean consent) {
    var used = status == CareLinkStatus.PENDING ? null : NOW.minusSeconds(10);
    var decision =
        status == CareLinkStatus.PENDING || status == CareLinkStatus.AWAITING_CONSENT
            ? null
            : NOW.minusSeconds(9);
    return rows.save(
        new CareLinkPersistenceEntity(
            UUID.randomUUID().toString(),
            UUID.randomUUID().toString(),
            adult,
            status,
            UUID.randomUUID().toString().substring(0, 20),
            NOW.plusSeconds(60),
            used,
            consent,
            decision,
            status == CareLinkStatus.CONFIRMED ? NOW.minusSeconds(8) : null,
            NOW.minusSeconds(20),
            NOW));
  }

  @Test
  void scans105ConfirmedCaregiversInDistinctOrderedPages() {
    var adult = UUID.randomUUID().toString();
    for (int i = 0; i < 105; i++) relationship(adult, CareLinkStatus.CONFIRMED, true);
    relationship(UUID.randomUUID().toString(), CareLinkStatus.CONFIRMED, true);
    entities.flush();
    entities.clear();
    var first = links.findAuthorizedCaregivers(adult, "", 100);
    var last = links.findAuthorizedCaregivers(adult, first.nextCursor(), 100);
    assertThat(first.caregiverIds()).hasSize(100);
    assertThat(last.caregiverIds()).hasSize(5);
    assertThat(last.nextCursor()).isNull();
    var ids = new ArrayList<>(first.caregiverIds());
    ids.addAll(last.caregiverIds());
    assertThat(ids).doesNotHaveDuplicates().isSorted();
  }

  @Test
  void unconfirmedWithdrawnAndMissingConsentNeverBecomeRecipients() {
    var adult = UUID.randomUUID().toString();
    var confirmed = relationship(adult, CareLinkStatus.CONFIRMED, true);
    relationship(adult, CareLinkStatus.PENDING, false);
    relationship(adult, CareLinkStatus.AWAITING_CONSENT, false);
    relationship(adult, CareLinkStatus.REVOKED, false);
    relationship(adult, CareLinkStatus.CONFIRMED, false);
    entities.flush();
    entities.clear();
    assertThat(links.findAuthorizedCaregivers(adult, "", 100).caregiverIds())
        .containsExactly(confirmed.getCaregiverId());
  }
}
