package com.tata.familymonitoring.domain.ports;

import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import java.util.Optional;

/** Port to the context that owns the older adult's contact data. */
public interface IContactChannelPort {

  Optional<ContactChannel> findContactChannel(Long olderAdultId);
}
