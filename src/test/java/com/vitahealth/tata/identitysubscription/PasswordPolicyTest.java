package com.vitahealth.tata.identitysubscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.identitysubscription.domain.model.commands.AuthenticateFamilyCommand;
import com.vitahealth.tata.identitysubscription.domain.model.commands.RegisterFamilyAccountCommand;
import com.vitahealth.tata.identitysubscription.domain.services.PasswordPolicy;
import org.junit.jupiter.api.Test;

/** Tests registration policy and the diagnostic handling of raw credentials. */
class PasswordPolicyTest {
  @Test
  void rejectsBlankAndShortPasswordsWithoutTrimmingValidPasswords() {
    assertThatThrownBy(() -> PasswordPolicy.requireValidRegistrationPassword("        "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> PasswordPolicy.requireValidRegistrationPassword("short"))
        .isInstanceOf(IllegalArgumentException.class);
    PasswordPolicy.requireValidRegistrationPassword("  valid password  ");
  }

  @Test
  void diagnosticOutputHidesCredentialsButAccessorsPreserveInput() {
    var register = new RegisterFamilyAccountCommand("Owner", "private@example.com", "secret-value");
    var login = new AuthenticateFamilyCommand("private@example.com", "secret-value");
    assertThat(register.toString()).doesNotContain("secret-value", "private@example.com");
    assertThat(login.toString()).doesNotContain("secret-value", "private@example.com");
    assertThat(register.password()).isEqualTo("secret-value");
    assertThat(login.password()).isEqualTo("secret-value");
  }
}
