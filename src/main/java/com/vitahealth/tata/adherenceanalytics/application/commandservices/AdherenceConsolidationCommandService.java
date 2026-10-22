package com.vitahealth.tata.adherenceanalytics.application.commandservices;

import com.vitahealth.tata.adherenceanalytics.domain.model.aggregates.AdherencePeriodSnapshot;
import com.vitahealth.tata.adherenceanalytics.domain.model.commands.ConsolidateWeeklyPeriodCommand;

/** Creates or returns fixed period evidence without exposing query responsibilities. */
public interface AdherenceConsolidationCommandService {
  /** Captures a period once; subsequent requests retain its stored historical evidence. */
  AdherencePeriodSnapshot handle(ConsolidateWeeklyPeriodCommand command);
}
