package com.tata.inventoryreplenishment.application.internal.queryservices;

import com.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.tata.inventoryreplenishment.application.internal.fakes.InMemoryInventoryRepository;
import com.tata.inventoryreplenishment.domain.model.aggregates.Inventory;
import com.tata.inventoryreplenishment.domain.model.queries.GetRemainingStockQuery;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class InventoryQueryServiceImplTest {
    private static final Instant NOW = Instant.parse("2026-10-06T13:00:00Z");

    @Test
    void returnsRemainingStockWithLowStockFlag() {
        var repository = new InMemoryInventoryRepository();
        repository.save(Inventory.registerInitial("medication-1", 4, 5, NOW));
        var service = new InventoryQueryServiceImpl(repository);

        var result = service.getRemainingStock(new GetRemainingStockQuery(" medication-1 "));

        assertEquals(4, result.remainingStock());
        assertTrue(result.lowStock());
        assertEquals(1, result.batches().size());
    }

    @Test
    void returnsNotFoundForUnknownMedication() {
        var service = new InventoryQueryServiceImpl(new InMemoryInventoryRepository());

        var exception = assertThrows(
                InventoryApplicationException.class,
                () -> service.getRemainingStock(new GetRemainingStockQuery("missing"))
        );

        assertEquals(InventoryApplicationException.Code.INVENTORY_NOT_FOUND, exception.code());
    }

    @Test
    void rejectsBlankMedicationId() {
        assertThrows(IllegalArgumentException.class, () -> new GetRemainingStockQuery(" "));
    }
}
