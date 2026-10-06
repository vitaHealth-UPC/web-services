package com.tata.inventoryreplenishment.application.internal.queryservices;

import com.tata.inventoryreplenishment.application.internal.InventoryApplicationException;
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

    public InventoryQueryServiceImpl(InventoryRepository repository) {
        this.repository = repository;
    }

    @Override
    public InventoryResult getRemainingStock(GetRemainingStockQuery query) {
        return repository.findByMedicationId(query.medicationId())
                .map(InventoryMapper::toResult)
                .orElseThrow(() -> new InventoryApplicationException(
                        InventoryApplicationException.Code.INVENTORY_NOT_FOUND,
                        "inventory not found"
                ));
    }
}
