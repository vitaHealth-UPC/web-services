package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession;
import com.vitahealth.tata.identitysubscription.application.authentication.AuthenticatedSession.Role;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/** Verifies that incomplete identity or missing relationship scope cannot become a principal. */
class AuthenticatedSessionTest {
  @Test
  void rejectsAdultAndSetupIdentitiesWithoutTheirLink() {
    for (var role : new Role[] {Role.OLDER_ADULT, Role.LINK_SETUP}) {
      assertThatThrownBy(() -> new AuthenticatedSession("owner", role, Instant.EPOCH, null))
          .isInstanceOf(IllegalArgumentException.class);
    }
  }

  @Test
  void caregiverIdentityDoesNotRequireAClinicalRelationship() {
    assertThat(new AuthenticatedSession("owner", Role.CAREGIVER, Instant.EPOCH, null).subjectId())
        .isEqualTo("owner");
    assertThatThrownBy(() -> new AuthenticatedSession(" ", Role.CAREGIVER, Instant.EPOCH, null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
