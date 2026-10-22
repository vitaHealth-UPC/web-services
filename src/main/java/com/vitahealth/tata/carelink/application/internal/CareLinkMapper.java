package com.vitahealth.tata.carelink.application.internal;

import com.vitahealth.tata.carelink.application.result.CareLinkResult;
import com.vitahealth.tata.carelink.application.result.OlderAdultProfileResult;
import com.vitahealth.tata.carelink.domain.model.aggregates.CareLink;
import com.vitahealth.tata.carelink.domain.model.aggregates.OlderAdultProfile;
import java.util.Objects;

/**
 * Projects domain evidence into application results without persistence or transport operations.
 */
public final class CareLinkMapper {
  private CareLinkMapper() {}

  /** Preserves relationship identity, state and decision timestamps for authorized consumers. */
  public static CareLinkResult toResult(CareLink link) {
    Objects.requireNonNull(link, "link");
    return new CareLinkResult(
        link.id(),
        link.caregiverId(),
        link.olderAdultId(),
        link.status(),
        link.linkingCode().value(),
        link.linkingCode().expiresAt(),
        link.linkingCode().usedAt(),
        link.consent().isGranted(),
        link.consent().recordedAt(),
        link.confirmedAt());
  }

  /** Projects profile data and a complete optional contact snapshot without computing age. */
  public static OlderAdultProfileResult toResult(OlderAdultProfile profile) {
    Objects.requireNonNull(profile, "profile");
    var contact = profile.emergencyContact();
    return new OlderAdultProfileResult(
        profile.id(),
        profile.registeredByCaregiverId(),
        profile.basicData().fullName(),
        profile.basicData().birthDate(),
        contact == null ? null : contact.name(),
        contact == null ? null : contact.relationship(),
        contact == null ? null : contact.phone(),
        profile.createdAt());
  }
}
