CREATE TABLE family_monitor_inventory_positions (
    family_monitor_id BIGINT NOT NULL REFERENCES family_monitors(id),
    medication_id VARCHAR(36) NOT NULL,
    inventory_id VARCHAR(36) NOT NULL,
    source_revision BIGINT NOT NULL CHECK (source_revision >= 0),
    remaining_stock INTEGER NOT NULL CHECK (remaining_stock >= 0),
    replenishment_threshold INTEGER NOT NULL CHECK (replenishment_threshold >= 0),
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_family_inventory_position UNIQUE (family_monitor_id, medication_id)
);
