package com.tata.inventoryreplenishment.domain;
import com.tata.inventoryreplenishment.domain.services.StockCoveragePolicy;
import com.tata.inventoryreplenishment.domain.model.entities.Batch;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
class StockCoveragePolicyTest {
 @Test void returnsCompleteDaysWithoutOverstatingCoverage() { assertEquals(2,StockCoveragePolicy.daysRemaining(5,2));assertEquals(0,StockCoveragePolicy.daysRemaining(1,2)); }
 @Test void unknownOrInactiveScheduleHasNoEstimate() { assertNull(StockCoveragePolicy.daysRemaining(10,null));assertNull(StockCoveragePolicy.daysRemaining(10,0)); }
 @Test void rejectsInvalidLotAndNormalizesOptionalNotes() { assertThrows(IllegalArgumentException.class,()->Batch.register(1,Instant.now(),"x".repeat(201)));assertNull(Batch.register(1,Instant.now()," ").lot()); }
}
