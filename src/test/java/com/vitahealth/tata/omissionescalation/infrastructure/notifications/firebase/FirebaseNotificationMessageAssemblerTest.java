package com.vitahealth.tata.omissionescalation.infrastructure.notifications.firebase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vitahealth.tata.omissionescalation.application.result.NotificationDispatchRequest;
import com.vitahealth.tata.omissionescalation.domain.model.valueobjects.NotificationKind;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/** Verifies exact data-contract mapping independently of Firebase and translated display text. */
class FirebaseNotificationMessageAssemblerTest {
  @ParameterizedTest
  @EnumSource(NotificationKind.class)
  void payloadIsVersionedImmutableAndStableAcrossRetryWithRotatedToken(NotificationKind kind) {
    var time = Instant.parse("2026-10-18T12:34:56.123456Z");
    var first =
        new NotificationDispatchRequest(
            "delivery", "intake", "adult", kind, "�cido f�lico 5 mg", time, "private-token");
    var retry =
        new NotificationDispatchRequest(
            "delivery", "intake", "adult", kind, "�cido f�lico 5 mg", time, "rotated-token");
    var message = FirebaseNotificationMessageAssembler.toMessage(first);
    var rotated = FirebaseNotificationMessageAssembler.toMessage(retry);
    assertThat(message.data())
        .isEqualTo(
            Map.of(
                "schemaVersion",
                "1",
                "deliveryId",
                "delivery",
                "intakeId",
                "intake",
                "olderAdultId",
                "adult",
                "kind",
                kind.name(),
                "medicationName",
                "�cido f�lico 5 mg",
                "scheduledAt",
                "2026-10-18T12:34:56.123456Z"));
    assertThat(rotated.data()).isEqualTo(message.data());
    assertThat(rotated.token()).isEqualTo("rotated-token");
    assertThatThrownBy(() -> message.data().put("kind", "changed"))
        .isInstanceOf(UnsupportedOperationException.class);
    assertThat(first.toString()).doesNotContain("private-token", "�cido");
    assertThat(message.toString()).doesNotContain("private-token", "�cido");
  }

  @Test
  void missingSourceTimeIsRejectedInsteadOfInventingNow() {
    assertThatThrownBy(
            () ->
                new NotificationDispatchRequest(
                    "delivery",
                    "intake",
                    "adult",
                    NotificationKind.CAREGIVER_ALERT,
                    "Medication",
                    null,
                    "token"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
