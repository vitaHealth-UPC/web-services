package com.vitahealth.tata.identitysubscription.infrastructure.authorization.sfs;

import static org.assertj.core.api.Assertions.assertThat;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import org.junit.jupiter.api.Test;

/** Checks public methods, protected variants and lookalike paths use the effective route policy. */
class SessionSecurityOpenApiCustomizerTest {
  @Test
  void operationsUseMethodAndExactPathRatherThanControllerWideSecurity() {
    var api =
        new OpenAPI()
            .paths(
                new Paths()
                    .addPathItem(
                        "/api/v1/accounts",
                        new PathItem().post(new Operation()).get(new Operation()))
                    .addPathItem(
                        "/api/v1/plans", new PathItem().get(new Operation()).post(new Operation()))
                    .addPathItem(
                        "/api/v1/sessions",
                        new PathItem().post(new Operation()).delete(new Operation()))
                    .addPathItem(
                        "/api/v1/care-links/{careLinkId}", new PathItem().get(new Operation()))
                    .addPathItem("/swagger-ui-impostor", new PathItem().get(new Operation())));
    new SessionSecurityOpenApiCustomizer().customise(api);
    api.getPaths()
        .forEach(
            (path, item) ->
                item.readOperationsMap()
                    .forEach(
                        (method, operation) -> {
                          if (PublicSessionEndpoints.matches(method.name(), path)) {
                            assertThat(operation.getSecurity()).isEmpty();
                          } else {
                            assertThat(operation.getSecurity()).hasSize(1);
                            assertThat(operation.getSecurity().getFirst())
                                .containsKey("bearerAuth");
                          }
                        }));
    assertThat(api.getPaths().get("/api/v1/accounts").getPost().getSecurity()).isEmpty();
    assertThat(api.getPaths().get("/api/v1/accounts").getGet().getSecurity()).hasSize(1);
  }
}
