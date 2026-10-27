package com.vitahealth.tata.familymonitoring.infrastructure.acl;

import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.AlertSeverity;
import com.vitahealth.tata.familymonitoring.domain.ports.AlertSeverityPort;
import com.vitahealth.tata.omissionescalation.interfaces.acl.OmissionContextFacade;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Translates published omission evidence without importing the source persistence model. */
@Component
public class AlertSeverityAdapter implements AlertSeverityPort {
  private final OmissionContextFacade omissions;

  public AlertSeverityAdapter(OmissionContextFacade omissions) {
    this.omissions = omissions;
  }

  @Override
  public Map<String, AlertSeverity> forIntakes(String olderAdultId, Set<String> intakeIds) {
    try {
      return omissions.escalationLevels(olderAdultId, intakeIds).entrySet().stream()
          .collect(
              Collectors.toUnmodifiableMap(
                  Map.Entry::getKey, entry -> AlertSeverity.fromEscalationLevel(entry.getValue())));
    } catch (IllegalArgumentException
        | IllegalStateException
        | NullPointerException invalidSource) {
      throw new InvalidMonitoringSourceException(invalidSource);
    }
  }
}
