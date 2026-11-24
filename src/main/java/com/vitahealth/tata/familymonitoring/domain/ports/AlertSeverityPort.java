package com.vitahealth.tata.familymonitoring.domain.ports;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertSeverity;
import java.util.Map;
import java.util.Set;

/** Reads priority evidence for explicitly selected intakes of one authorized adult. */
public interface AlertSeverityPort {
  /** Missing cases are omitted from the map, never assumed to have standard priority. */
  Map<String, AlertSeverity> forIntakes(String olderAdultId, Set<String> intakeIds);
}
