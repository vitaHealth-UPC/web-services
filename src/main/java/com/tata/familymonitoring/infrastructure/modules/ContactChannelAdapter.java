package com.tata.familymonitoring.infrastructure.modules;

import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.tata.familymonitoring.domain.ports.IContactChannelPort;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Reads the older adult's emergency contact from Care Link. That module does not expose its public
 * contract yet, so no contact channel is reported.
 */
@Component
@Profile("!dev")
public class ContactChannelAdapter implements IContactChannelPort {

  @Override
  public Optional<ContactChannel> findContactChannel(String olderAdultId) {
    return Optional.empty();
  }
}
