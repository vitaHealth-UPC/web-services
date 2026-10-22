package com.vitahealth.tata.identitysubscription.interfaces.rest;

import com.vitahealth.tata.identitysubscription.application.commandservices.ChangeSubscriptionCommandService;
import com.vitahealth.tata.identitysubscription.application.queryservices.GetCurrentSubscriptionQueryService;
import com.vitahealth.tata.identitysubscription.application.queryservices.ListAvailablePlansQueryService;
import com.vitahealth.tata.identitysubscription.interfaces.rest.resources.ChangeSubscriptionResource;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.ChangeSubscriptionCommandFromResourceAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.IdentityErrorResponseAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.PlanResourceFromResultAssembler;
import com.vitahealth.tata.identitysubscription.interfaces.rest.transform.SubscriptionResourceFromResultAssembler;
import com.vitahealth.tata.shared.interfaces.rest.transform.ResponseEntityAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Exposes the public plan catalog and authorized account subscription operations. */
@RestController
@RequestMapping("/api/v1")
@Tag(name = "Plans and subscriptions")
public class SubscriptionsController {
  private final IdentityErrorResponseAssembler errors;
  private final ListAvailablePlansQueryService listPlans;
  private final GetCurrentSubscriptionQueryService getSubscription;
  private final ChangeSubscriptionCommandService changeSubscription;

  /** Injects separate catalog, account query and subscription command contracts. */
  public SubscriptionsController(
      ListAvailablePlansQueryService listPlans,
      GetCurrentSubscriptionQueryService getSubscription,
      ChangeSubscriptionCommandService changeSubscription,
      IdentityErrorResponseAssembler errors) {
    this.errors = errors;
    this.listPlans = listPlans;
    this.getSubscription = getSubscription;
    this.changeSubscription = changeSubscription;
  }

  /** Returns available plans without requiring an account session. */
  @GetMapping("/plans")
  @Operation(summary = "List the currently available Tata plans")
  public ResponseEntity<?> listPlans() {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(listPlans.handle()),
        values -> values.stream().map(PlanResourceFromResultAssembler::toResource).toList(),
        HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }

  /** Returns the subscription of the account authorized by the resource boundary. */
  @GetMapping("/accounts/{accountId}/subscription")
  @Operation(summary = "Get the current subscription and enabled capabilities for an account")
  public ResponseEntity<?> current(@PathVariable String accountId) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(getSubscription.handle(accountId)),
        SubscriptionResourceFromResultAssembler::toResource,
        HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }

  /** Applies a plan change through the command contract and its explicit failure mapping. */
  @PutMapping("/accounts/{accountId}/subscription")
  @Operation(summary = "Activate or change the subscription plan for an account")
  public ResponseEntity<?> change(
      @PathVariable String accountId, @Valid @RequestBody ChangeSubscriptionResource resource) {
    return ResponseEntityAssembler.toResponseEntityFromResult(
        errors.localize(
            changeSubscription.handle(
                ChangeSubscriptionCommandFromResourceAssembler.toCommand(accountId, resource))),
        SubscriptionResourceFromResultAssembler::toResource,
        HttpStatus.OK,
        IdentityErrorResponseAssembler::status);
  }
}
