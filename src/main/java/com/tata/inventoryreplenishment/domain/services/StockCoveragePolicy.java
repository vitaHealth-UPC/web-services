package com.tata.inventoryreplenishment.domain.services;
public final class StockCoveragePolicy {
 private StockCoveragePolicy() {}
 public static Integer daysRemaining(int stock, Integer dailyUnits) {
  if(stock<0) throw new IllegalArgumentException("stock cannot be negative");
  return dailyUnits==null || dailyUnits<=0 ? null : stock/dailyUnits;
 }
}
