# Inventory registration

`POST /api/v1/inventories` accepts `medicationId`, `initialQuantity` and `replenishmentThreshold`. Initial stock requires an existing active medication, as specified by US-40. The inventory context queries the public treatment application service through its own catalog port; it does not access treatment persistence.

Unknown medications return HTTP 404 with `MEDICATION_NOT_FOUND`. Inactive medications return HTTP 409 with `MEDICATION_INACTIVE`. Both failures leave stock and batches unchanged. An already registered inventory returns HTTP 409 with `INVENTORY_ALREADY_EXISTS`. Existing successful response fields remain unchanged.

Medication deactivation does not delete existing stock or historical consumption. Replenishment and stock queries retain their existing contracts.

## Batch metadata and coverage

Replenishment accepts optional `lot` (a lot identifier or note, up to 200 characters). The normalized value is persisted and returned on the registered batch. Older quantity-only requests remain valid.

Inventory responses include nullable `daysRemaining` and `dailyConsumptionUnits`. Coverage is the number of complete days at the current active daily scheduled-intake rate, using the inventory policy of one unit per confirmed intake. Multiple active treatments for the same medication contribute all their scheduled times. No medical dose text is interpreted. Without an active medication and a known schedule the estimate is null; it is not zero.
