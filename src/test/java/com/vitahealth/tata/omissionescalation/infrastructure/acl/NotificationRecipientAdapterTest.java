package com.vitahealth.tata.omissionescalation.infrastructure.acl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.identitysubscription.application.result.PushDeviceAddressResult;
import com.vitahealth.tata.identitysubscription.application.result.PushDevicePageResult;
import com.vitahealth.tata.identitysubscription.interfaces.acl.PushDeviceContextFacade;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Verifies Omission cannot resolve caregiver addresses without current pair authorization. */
class NotificationRecipientAdapterTest {
  @Test
  void unrelatedPairNeverQueriesDeviceAddresses() {
    var links = mock(CareLinkContextFacade.class);
    var devices = mock(PushDeviceContextFacade.class);
    var page =
        new NotificationRecipientAdapter(links, devices)
            .findCaregiverDevices("adult", "unrelated", "", 100);
    assertThat(page.devices()).isEmpty();
    assertThat(page.nextCursor()).isNull();
    verifyNoInteractions(devices);
  }

  @Test
  void authorizedPairPreservesDeviceAndCursorWithoutLeakingNeighboringModels() {
    var links = mock(CareLinkContextFacade.class);
    var devices = mock(PushDeviceContextFacade.class);
    when(links.isAuthorized("caregiver", "adult")).thenReturn(true);
    when(devices.findCaregiverDevices("caregiver", "", 100))
        .thenReturn(
            new PushDevicePageResult(
                List.of(new PushDeviceAddressResult("installation", "session", "private-token")),
                "cursor"));
    var page =
        new NotificationRecipientAdapter(links, devices)
            .findCaregiverDevices("adult", "caregiver", "", 100);
    assertThat(page.nextCursor()).isEqualTo("cursor");
    assertThat(page.devices()).hasSize(1);
    assertThat(page.devices().getFirst().token()).isEqualTo("private-token");
    assertThat(page.toString()).doesNotContain("private-token");
  }

  @Test
  void withdrawnPairCannotResolveASingleQueuedDevice() {
    var links = mock(CareLinkContextFacade.class);
    var devices = mock(PushDeviceContextFacade.class);
    assertThat(
            new NotificationRecipientAdapter(links, devices)
                .findCaregiverDevice("adult", "caregiver", "installation", "original-session"))
        .isEmpty();
    verifyNoInteractions(devices);
  }
}
