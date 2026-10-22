package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.identitysubscription.application.result.PlanResult;
import com.vitahealth.tata.identitysubscription.application.result.SessionResult;
import com.vitahealth.tata.identitysubscription.domain.model.valueobjects.PlanCapability;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashSet;
import org.junit.jupiter.api.Test;

/** Verifies immutable application projections and credential-safe diagnostic output. */
class IdentityResultContractTest {
  @Test
  void planRetainsItsCapabilitySnapshotWhenInputChanges() {
    var capability = PlanCapability.values()[0];
    var source = new HashSet<PlanCapability>();
    source.add(capability);
    var result = new PlanResult("BASIC", "Basic", BigDecimal.ZERO, "PEN", source);
    source.clear();

    assertThat(result.capabilities()).containsExactly(capability);
    assertThatThrownBy(() -> result.capabilities().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void sessionDiagnosticOutputDoesNotExposeCredential() {
    var session = new SessionResult("owner", "secret-bearer-token", Instant.EPOCH);
    assertThat(session.accessToken()).isEqualTo("secret-bearer-token");
    assertThat(session.toString()).doesNotContain("secret-bearer-token").contains("<redacted>");
  }
}
