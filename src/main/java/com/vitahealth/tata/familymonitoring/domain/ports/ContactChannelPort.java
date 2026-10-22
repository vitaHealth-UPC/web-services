package com.vitahealth.tata.familymonitoring.domain.ports;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import java.util.Optional;

/** Port to the context that owns the older adult's contact data. */
public interface ContactChannelPort {

  /** Returns the registered phone, or absence when the matching profile has no usable contact. */
  Optional<ContactChannel> findContactChannel(String olderAdultId);
}
