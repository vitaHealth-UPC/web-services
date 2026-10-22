package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import java.util.List;
import org.springdoc.core.customizers.GlobalOpenApiCustomizer;
import org.springframework.stereotype.Component;

/** Documents operation authentication using the same route policy enforced by session filters. */
@Component
public final class SessionSecurityOpenApiCustomizer implements GlobalOpenApiCustomizer {
  /**
   * Assigns bearer authentication to protected operations and an explicit empty requirement to
   * public operations, including in grouped API documents.
   *
   * @param openApi generated API document whose operations are ready for customization
   */
  @Override
  public void customise(OpenAPI openApi) {
    if (openApi.getPaths() == null) return;
    openApi
        .getPaths()
        .forEach(
            (path, item) ->
                item.readOperationsMap()
                    .forEach(
                        (method, operation) -> {
                          operation.setSecurity(
                              PublicSessionEndpoints.matches(method.name(), path)
                                  ? List.of()
                                  : List.of(new SecurityRequirement().addList("bearerAuth")));
                        }));
  }
}
