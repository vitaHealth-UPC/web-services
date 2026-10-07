package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.tata.familymonitoring.domain.ports.IContactChannelPort;
import com.tata.familymonitoring.domain.model.valueobjects.ContactChannelType;
import com.tata.carelink.application.queryservices.CareLinkQueryService;
import com.tata.carelink.application.internal.CareLinkApplicationException;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads the registered emergency contact through the Care Link application contract.
 */
@Component
public class ContactChannelAdapter implements IContactChannelPort {
  private final CareLinkQueryService profiles;
  public ContactChannelAdapter(CareLinkQueryService profiles) { this.profiles = profiles; }

  @Override
  public Optional<ContactChannel> findContactChannel(String olderAdultId) {
    try {
      var phone = profiles.getOlderAdult(olderAdultId).emergencyContactPhone();
      return phone == null || phone.isBlank() ? Optional.empty()
          : Optional.of(new ContactChannel(ContactChannelType.PHONE, phone));
    } catch (CareLinkApplicationException exception) {
      if (exception.code() == CareLinkApplicationException.Code.OLDER_ADULT_NOT_FOUND) return Optional.empty();
      throw exception;
    }
  }
}
