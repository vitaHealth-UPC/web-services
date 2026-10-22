package com.vitahealth.tata.carelink;

import static org.assertj.core.api.Assertions.assertThat;

import com.vitahealth.tata.carelink.application.queryservices.CareLinkQueryService;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import com.vitahealth.tata.carelink.domain.model.queries.GetConfirmedCareLinkSummariesQuery;
import com.vitahealth.tata.carelink.domain.model.valueobjects.LinkingCode;
import com.vitahealth.tata.carelink.domain.model.valueobjects.OlderAdultBasicData;
import com.vitahealth.tata.carelink.domain.repositories.CareLinkRepository;
import com.vitahealth.tata.carelink.domain.repositories.OlderAdultProfileRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

/** Measures actual SQL after clearing persistence context so per-row profile reads cannot hide. */
@SpringBootTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@ActiveProfiles("test")
@Transactional
class ConfirmedCareLinkBatchQueriesIntegrationTest {
  @Autowired private CareLinkQueryService queries;
  @Autowired private CareLinkRepository links;
  @Autowired private OlderAdultProfileRepository profiles;
  @Autowired private EntityManager entityManager;
  @Autowired private EntityManagerFactory factory;

  @Test
  void severalRelationshipsLoadInTwoStatementsAndPreserveTheirDisplayOrder() {
    var caregiver = UUID.randomUUID().toString();
    var names = new ArrayList<String>();
    var now = Instant.parse("2026-10-13T12:00:00Z");
    for (int index = 0; index < 5; index++) {
      var name = "Adult " + index;
      names.addFirst(name);
      var profile =
          profiles.save(
              OlderAdultProfile.register(
                  caregiver, new OlderAdultBasicData(name, LocalDate.of(1958, 1, 1)), null, now));
      var code = "BATCH-" + index;
      var link =
          CareLink.createPending(
              caregiver, profile.id(), LinkingCode.issue(code, now, Duration.ofMinutes(15)), now);
      link.accept(code, now.plusSeconds(index + 1));
      link.registerConsent(true, now.plusSeconds(index + 1));
      link.confirm(now.plusSeconds(index + 1));
      links.save(link);
    }
    entityManager.flush();
    entityManager.clear();
    var statistics = factory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    var result =
        queries
            .handle(new GetConfirmedCareLinkSummariesQuery(caregiver))
            .toOptional()
            .orElseThrow();
    assertThat(result).extracting(item -> item.olderAdultName()).containsExactlyElementsOf(names);
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(2);
  }

  @Test
  void emptyRelationshipListDoesNotIssueAnUnusedProfileQuery() {
    entityManager.clear();
    var statistics = factory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();
    assertThat(
            queries
                .handle(new GetConfirmedCareLinkSummariesQuery(UUID.randomUUID().toString()))
                .toOptional()
                .orElseThrow())
        .isEmpty();
    assertThat(statistics.getPrepareStatementCount()).isEqualTo(1);
  }
}
