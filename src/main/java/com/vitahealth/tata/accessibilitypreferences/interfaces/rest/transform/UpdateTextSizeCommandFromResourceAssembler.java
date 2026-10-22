package com.vitahealth.tata.accessibilitypreferences.interfaces.rest.transform;

import com.vitahealth.tata.accessibilitypreferences.domain.model.commands.UpdateTextSizeCommand;
import com.vitahealth.tata.accessibilitypreferences.interfaces.rest.resources.UpdateTextSizeResource;

/** Converts the transport text-size choice into its owner-scoped domain command. */
public final class UpdateTextSizeCommandFromResourceAssembler {
  private UpdateTextSizeCommandFromResourceAssembler() {}

  /** Copies the validated choice and path owner without applying or persisting a change. */
  public static UpdateTextSizeCommand toCommandFromResource(
      String userId, UpdateTextSizeResource resource) {
    return new UpdateTextSizeCommand(userId, resource.textSize());
  }
}
