package com.vitahealth.tata.familymonitoring.infrastructure.acl;

import com.vitahealth.tata.carelink.interfaces.acl.CareLinkContextFacade;
import com.vitahealth.tata.familymonitoring.application.exceptions.InvalidMonitoringSourceException;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannelType;
import com.vitahealth.tata.familymonitoring.domain.ports.ContactChannelPort;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** Reads the registered emergency contact through the Care Link application contract. */
@Component
public class ContactChannelAdapter implements ContactChannelPort {
  private final CareLinkContextFacade profiles;

  /** Injects the public Care Link profile contract. */
  public ContactChannelAdapter(CareLinkContextFacade profiles) {
    this.profiles = profiles;
  }

  /** Returns the registered phone contact when the profile supplies one. */
  @Override
  public Optional<ContactChannel> findContactChannel(String olderAdultId) {
    try {
      return profiles
          .findOlderAdult(olderAdultId)
          .map(
              profile -> {
                if (!olderAdultId.equals(profile.id()))
                  throw new IllegalStateException("source profile differs from requested adult");
                return profile.emergencyContactPhone();
              })
          .filter(phone -> !phone.isBlank())
          .map(phone -> new ContactChannel(ContactChannelType.PHONE, phone));
    } catch (IllegalArgumentException
        | IllegalStateException
        | NullPointerException invalidSource) {
      throw new InvalidMonitoringSourceException(invalidSource);
    }
  }
}
