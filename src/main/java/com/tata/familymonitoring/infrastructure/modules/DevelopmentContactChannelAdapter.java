package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.tata.familymonitoring.domain.model.valueobjects.ContactChannelType;
import com.tata.familymonitoring.domain.ports.IContactChannelPort;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Sample contact for the seeded older adult (id 1) so the endpoint can be tried in dev. Replace it
 * with a call to Care Link when that module exposes the emergency contact.
 */
@Component
@Profile("dev")
public class DevelopmentContactChannelAdapter implements IContactChannelPort {

  private static final Long SAMPLE_OLDER_ADULT_ID = 1L;

  @Override
  public Optional<ContactChannel> findContactChannel(Long olderAdultId) {
    if (SAMPLE_OLDER_ADULT_ID.equals(olderAdultId)) {
      return Optional.of(new ContactChannel(ContactChannelType.PHONE, "+51 999 888 777"));
    }
    return Optional.empty();
  }
}
