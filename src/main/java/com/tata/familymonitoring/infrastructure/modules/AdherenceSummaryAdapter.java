package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.model.valueobjects.AdherenceSnapshot;
import com.tata.familymonitoring.domain.ports.IAdherenceSummaryPort;
import org.springframework.stereotype.Component;

/**
 * Reads adherence indicators from Adherence Analytics. That module does not expose its public
 * contract yet, so the adapter reports an empty week.
 */
@Component
public class AdherenceSummaryAdapter implements IAdherenceSummaryPort {

  @Override
  public AdherenceSnapshot getWeeklySummary(Long olderAdultId) {
    return new AdherenceSnapshot(0, 0);
  }
}
