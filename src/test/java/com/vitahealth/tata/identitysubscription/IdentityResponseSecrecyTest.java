package com.vitahealth.tata.identitysubscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.vitahealth.tata.identitysubscription.domain.model.commands.VerifyEmailCommand;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.AccountResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.AuthenticatedAccountResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.PinSessionResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.PinSignInResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterAccountResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.RegisterPinResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.SignInResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.VerifyEmailResource;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Ensures diagnostic representations cannot expose credentials carried by HTTP responses. */
class IdentityResponseSecrecyTest {
  @Test
  void requestDiagnosticsRedactPasswordsChallengesAndPins() {
    var requests =
        List.of(
            new RegisterAccountResource("Private Name", "private@example.com", "secret-password"),
            new SignInResource("private@example.com", "secret-password"),
            new VerifyEmailResource("private@example.com", "123456"),
            new RegisterPinResource("adult", "1234"),
            new PinSignInResource("adult", "1234"));
    for (var request : requests) {
      assertFalse(request.toString().contains("private@example.com"));
      assertFalse(request.toString().contains("secret-password"));
      assertFalse(request.toString().contains("1234"));
    }
  }

  @Test
  void accountDiagnosticsRedactPersonalDataAndBearer() {
    var resource =
        new AccountResource(
            "id",
            "Private Name",
            "private@example.com",
            "ACTIVE",
            "secret-bearer",
            Instant.parse("2026-10-12T20:00:00Z"));
    assertFalse(resource.toString().contains("secret-bearer"));
    assertFalse(resource.toString().contains("private@example.com"));
    assertFalse(resource.toString().contains("Private Name"));
    assertEquals("secret-bearer", resource.accessToken());
  }

  @Test
  void signInDiagnosticsRedactBearerWithoutChangingResponseAccessor() {
    var resource = new AuthenticatedAccountResource("id", "secret-bearer", Instant.now());
    assertFalse(resource.toString().contains("secret-bearer"));
    assertEquals("secret-bearer", resource.accessToken());
    var pinSession = new PinSessionResource("adult", "secret-bearer", Instant.now());
    assertFalse(pinSession.toString().contains("secret-bearer"));
    assertEquals("secret-bearer", pinSession.accessToken());
    assertFalse(
        new VerifyEmailCommand("private@example.com", "123456").toString().contains("123456"));
  }
}
