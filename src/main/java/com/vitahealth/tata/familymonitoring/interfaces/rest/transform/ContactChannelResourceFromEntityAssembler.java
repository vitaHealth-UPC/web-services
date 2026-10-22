package com.vitahealth.tata.familymonitoring.interfaces.rest.transform;

import com.vitahealth.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.vitahealth.tata.familymonitoring.interfaces.rest.resources.ContactChannelResource;

/** Maps the authorized contact value object without changing its destination format. */
public final class ContactChannelResourceFromEntityAssembler {

  private ContactChannelResourceFromEntityAssembler() {}

  /** Copies the channel type and original destination to the response. */
  public static ContactChannelResource toResourceFromEntity(ContactChannel channel) {
    return new ContactChannelResource(channel.type(), channel.value());
  }
}
