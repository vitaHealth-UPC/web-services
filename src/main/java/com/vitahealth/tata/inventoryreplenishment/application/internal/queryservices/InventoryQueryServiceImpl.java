package com.vitahealth.tata.inventoryreplenishment.application.internal.queryservices;

import com.vitahealth.tata.inventoryreplenishment.application.InventoryApplicationException;
import com.vitahealth.tata.inventoryreplenishment.application.internal.InventoryMapper;
import com.vitahealth.tata.inventoryreplenishment.application.internal.outboundservices.MedicationCatalog;
import com.vitahealth.tata.inventoryreplenishment.application.queryservices.InventoryQueryService;
import com.vitahealth.tata.inventoryreplenishment.application.result.InventoryResult;
import com.vitahealth.tata.inventoryreplenishment.domain.model.queries.GetRemainingStockQuery;
import com.vitahealth.tata.inventoryreplenishment.domain.repositories.InventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reads inventory evidence and coverage through the Treatment context contract. */
@Service
@Transactional(readOnly = true)
public class InventoryQueryServiceImpl implements InventoryQueryService {
  private final InventoryRepository repository;
  private final MedicationCatalog medications;

  /** Injects stock storage and the public medication coverage contract. */
  public InventoryQueryServiceImpl(InventoryRepository repository, MedicationCatalog medications) {
    this.repository = repository;
    this.medications = medications;
  }

  /** Reads stored inventory and calculates coverage without mutating stock or emitting events. */
  @Override
  public InventoryResult getRemainingStock(GetRemainingStockQuery query) {
    return repository
        .findByMedicationId(query.medicationId())
        .map(
            inventory ->
                InventoryMapper.toResult(
                    inventory, medications.dailyConsumptionUnits(inventory.medicationId())))
        .orElseThrow(
            () ->
                new InventoryApplicationException(
                    InventoryApplicationException.Code.INVENTORY_NOT_FOUND, "inventory not found"));
  }
}
