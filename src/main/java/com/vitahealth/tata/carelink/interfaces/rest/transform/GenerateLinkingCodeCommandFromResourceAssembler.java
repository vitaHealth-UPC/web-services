package com.vitahealth.tata.carelink.interfaces.rest.transform;

import com.vitahealth.tata.carelink.domain.model.commands.GenerateLinkingCodeCommand;
import com.vitahealth.tata.carelink.interfaces.rest.resources.GenerateLinkingCodeResource;

/** Converts code issuance input into its domain command. */
public final class GenerateLinkingCodeCommandFromResourceAssembler {
  private GenerateLinkingCodeCommandFromResourceAssembler() {}

  /** Converts code issuance input into its domain command. */
  public static GenerateLinkingCodeCommand toCommand(GenerateLinkingCodeResource resource) {
    return new GenerateLinkingCodeCommand(resource.caregiverId(), resource.olderAdultId());
  }
}
