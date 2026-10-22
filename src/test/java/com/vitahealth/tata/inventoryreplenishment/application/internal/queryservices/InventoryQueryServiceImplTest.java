package com.vitahealth.tata.inventoryreplenishment.application.internal.queryservices;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.vitahealth.tata.inventoryreplenishment.application.internal.fakes.InMemoryInventoryRepository;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog;
import com.vitahealth.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.vitahealth.tata.inventoryreplenishment.domain.model.queries.GetRemainingStockQuery;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventoryQueryServiceImplTest {
  private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");

  @Test
  void returnsRemainingStockWithLowStockFlag() {
    var repository = new InMemoryInventoryRepository();
    repository.save(Inventory.registerInitial("medication-1", 4, 5, NOW));
    var service =
        new InventoryQueryServiceImpl(repository, catalog(MedicationCatalog.Availability.ACTIVE));

    var result = service.getRemainingStock(new GetRemainingStockQuery(" medication-1 "));

    assertEquals(4, result.remainingStock());
    assertTrue(result.lowStock());
    assertEquals(1, result.batches().size());
  }

  @Test
  void returnsNotFoundForUnknownMedication() {
    var service =
        new InventoryQueryServiceImpl(
            new InMemoryInventoryRepository(), catalog(MedicationCatalog.Availability.ACTIVE));

    var exception =
        assertThrows(
            InventoryApplicationException.class,
            () -> service.getRemainingStock(new GetRemainingStockQuery("missing")));

    assertEquals(InventoryApplicationException.Code.INVENTORY_NOT_FOUND, exception.code());
  }

  @Test
  void rejectsBlankMedicationId() {
    assertThrows(IllegalArgumentException.class, () -> new GetRemainingStockQuery(" "));
  }

  private static MedicationCatalog catalog(MedicationCatalog.Availability availability) {
    var catalog = mock(MedicationCatalog.class);
    when(catalog.availability(anyString())).thenReturn(availability);
    when(catalog.dailyConsumptionUnits(anyString())).thenReturn(null);
    return catalog;
  }
}
