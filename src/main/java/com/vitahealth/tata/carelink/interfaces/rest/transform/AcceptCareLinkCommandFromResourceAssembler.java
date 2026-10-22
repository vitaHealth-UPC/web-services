package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.domain.model.commands.AcceptCareLinkCommand;
import com.vitahealth.tata.carelink.interfaces.rest.resources.AcceptCareLinkResource;

/** Converts supplied linking evidence into the acceptance command. */
public final class AcceptCareLinkCommandFromResourceAssembler {
  private AcceptCareLinkCommandFromResourceAssembler() {}

  /** Converts supplied linking evidence into the acceptance command. */
  public static AcceptCareLinkCommand toCommand(AcceptCareLinkResource resource) {
    return new AcceptCareLinkCommand(resource.caregiverId(), resource.code());
  }
}
