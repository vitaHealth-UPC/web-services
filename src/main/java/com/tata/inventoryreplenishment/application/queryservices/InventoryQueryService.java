package com.tata.inventoryreplenishment.application.queryservices;

import com.tata.inventoryreplenishment.application.models.InventoryResult;
import com.tata.inventoryreplenishment.domain.model.queries.GetRemainingStockQuery;

public interface InventoryQueryService {
    InventoryResult getRemainingStock(GetRemainingStockQuery query);
}
