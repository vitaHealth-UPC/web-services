package com.vitahealth.tata.inventoryreplenishment.domain.services;

/** Calculates complete days of availability from stock and scheduled inventory units per day. */
public final class StockCoveragePolicy {
  private StockCoveragePolicy() {}

  /** Returns null for unknown or nonpositive consumption rates; zero stock has zero known days. */
  public static Integer daysRemaining(int stock, Integer dailyUnits) {
    if (stock < 0) throw new IllegalArgumentException("stock cannot be negative");
    return dailyUnits == null || dailyUnits <= 0 ? null : stock / dailyUnits;
  }
}
