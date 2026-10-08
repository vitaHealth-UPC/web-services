package com.tata.familymonitoring.application.internal.outboundservices.acl;

import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.tata.familymonitoring.domain.ports.IContactChannelPort;
import com.tata.familymonitoring.domain.model.valueobjects.ContactChannelType;
import com.tata.carelink.interfaces.acl.CareLinkContextFacade;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Reads the registered emergency contact through the Care Link application contract.
 */
@Component
public class ContactChannelAdapter implements IContactChannelPort {
  private final CareLinkContextFacade profiles;
  public ContactChannelAdapter(CareLinkContextFacade profiles) { this.profiles = profiles; }

  @Override
  public Optional<ContactChannel> findContactChannel(String olderAdultId) {
    return profiles.findOlderAdult(olderAdultId)
        .map(profile -> profile.emergencyContactPhone())
        .filter(phone -> !phone.isBlank())
        .map(phone -> new ContactChannel(ContactChannelType.PHONE, phone));
  }
}
