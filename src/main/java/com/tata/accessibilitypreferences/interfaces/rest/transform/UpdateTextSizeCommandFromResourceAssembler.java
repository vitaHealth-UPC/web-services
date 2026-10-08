package com.tata.accessibilitypreferences.interfaces.rest.transform;

import com.tata.accessibilitypreferences.domain.model.commands.UpdateTextSizeCommand;
import com.tata.accessibilitypreferences.interfaces.rest.resources.UpdateTextSizeResource;

public final class UpdateTextSizeCommandFromResourceAssembler {
    private UpdateTextSizeCommandFromResourceAssembler() {}

    public static UpdateTextSizeCommand toCommandFromResource(String userId, UpdateTextSizeResource resource) {
        return new UpdateTextSizeCommand(userId, resource.textSize());
    }
}
