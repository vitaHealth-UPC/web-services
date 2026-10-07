package com.tata.familymonitoring.domain.model.valueobjects;

/** Weekly adherence indicators calculated by Adherence Analytics. */
public record AdherenceSnapshot(int confirmedIntakes, int totalIntakes) {
}
