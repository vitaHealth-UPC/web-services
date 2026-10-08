package com.tata.inventoryreplenishment.application.internal.queryservices;

import com.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.tata.inventoryreplenishment.application.internal.InventoryMapper;
import com.tata.inventoryreplenishment.application.models.InventoryResult;
import com.tata.inventoryreplenishment.application.queryservices.InventoryQueryService;
import com.tata.inventoryreplenishment.domain.model.queries.GetRemainingStockQuery;
import com.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {
    private final InventoryRepository repository;
    private final com.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog medications;

    public InventoryQueryServiceImpl(InventoryRepository repository) { this(repository, id -> com.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog.Availability.ACTIVE); }
    @org.springframework.beans.factory.annotation.Autowired
    public InventoryQueryServiceImpl(InventoryRepository repository, com.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog medications) {
        this.repository = repository; this.medications=medications;
    }

    @Override
    public InventoryResult getRemainingStock(GetRemainingStockQuery query) {
        return repository.findByMedicationId(query.medicationId())
                .map(inventory -> InventoryMapper.toResult(inventory,medications.dailyConsumptionUnits(inventory.medicationId())))
                .orElseThrow(() -> new InventoryApplicationException(
                        InventoryApplicationException.Code.INVENTORY_NOT_FOUND,
                        "inventory not found"
                ));
    }
}
