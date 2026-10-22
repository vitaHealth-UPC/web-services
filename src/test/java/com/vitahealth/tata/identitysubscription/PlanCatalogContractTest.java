package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.vitahealth.tata.identitysubscription.domain.services.PlanCatalog;
import org.junit.jupiter.api.Test;

/** Verifies lookup normalization is consistent with domain catalog identifiers. */
class PlanCatalogContractTest {
  @Test
  void resolvesNormalizedCodeAndReturnsAbsenceForMissingOrUnknownIdentifiers() {
    assertEquals(
        PlanCatalog.FAMILY, PlanCatalog.findByCode("\u2003family\u2003").orElseThrow().code());
    assertTrue(PlanCatalog.findByCode(null).isEmpty());
    assertTrue(PlanCatalog.findByCode("\u2003").isEmpty());
    assertTrue(PlanCatalog.findByCode("unknown").isEmpty());
  }
}
