package com.vitahealth.tata.inventoryreplenishment.application.queryservices;

import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryResult;
import com.vitahealth.tata.inventoryreplenishment.domain.model.queries.GetRemainingStockQuery;

/** Reads immutable inventory evidence; request authorization belongs to the calling boundary. */
public interface InventoryQueryService {
  /** Returns current stock, receipts and optional coverage, or reports absent inventory. */
  InventoryResult getRemainingStock(GetRemainingStockQuery query);
}
