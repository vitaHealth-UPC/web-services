package com.tata.familymonitoring.interfaces.rest.transform;

import com.tata.familymonitoring.domain.model.valueobjects.ContactChannel;
import com.tata.familymonitoring.interfaces.rest.resources.ContactChannelResource;

public final class ContactChannelResourceFromEntityAssembler {

  private ContactChannelResourceFromEntityAssembler() {
  }

  public static ContactChannelResource toResourceFromEntity(ContactChannel channel) {
    return new ContactChannelResource(channel.type(), channel.value());
  }
}
