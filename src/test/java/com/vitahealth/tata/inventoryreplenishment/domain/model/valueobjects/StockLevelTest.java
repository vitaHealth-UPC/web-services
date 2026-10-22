package com.vitahealth.tata.inventoryreplenishment.domain.model.valueobjects;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class StockLevelTest {

  @Test
  void isLowWhenRemainingReachesThreshold() {
    assertTrue(new StockLevel(5, 5).isLow());
    assertTrue(new StockLevel(4, 5).isLow());
    assertFalse(new StockLevel(6, 5).isLow());
  }

  @Test
  void rejectsNegativeRemainingStock() {
    assertThrows(IllegalArgumentException.class, () -> new StockLevel(-1, 5));
  }

  @Test
  void rejectsNegativeThreshold() {
    assertThrows(IllegalArgumentException.class, () -> new StockLevel(10, -1));
  }
}
