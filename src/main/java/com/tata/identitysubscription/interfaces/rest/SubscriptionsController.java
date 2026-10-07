package com.tata.identitysubscription.interfaces.rest;

import com.tata.identitysubscription.application.commandservices.ChangeSubscriptionCommandService;
import com.tata.identitysubscription.application.queryservices.GetCurrentSubscriptionQueryService;
import com.tata.identitysubscription.application.queryservices.ListAvailablePlansQueryService;
import com.tata.identitysubscription.interfaces.rest.resources.ChangeSubscriptionResource;
import com.tata.identitysubscription.interfaces.rest.resources.PlanResource;
import com.tata.identitysubscription.interfaces.rest.resources.SubscriptionResource;
import com.tata.identitysubscription.interfaces.rest.transform.SubscriptionResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Plans and subscriptions")
public class SubscriptionsController {
    private final ListAvailablePlansQueryService listPlans;
    private final GetCurrentSubscriptionQueryService getSubscription;
    private final ChangeSubscriptionCommandService changeSubscription;

    public SubscriptionsController(
            ListAvailablePlansQueryService listPlans,
            GetCurrentSubscriptionQueryService getSubscription,
            ChangeSubscriptionCommandService changeSubscription
    ) {
        this.listPlans = listPlans;
        this.getSubscription = getSubscription;
        this.changeSubscription = changeSubscription;
    }

    @GetMapping("/plans")
    @Operation(summary = "List the currently available Tata plans")
    public List<PlanResource> listPlans() {
        return listPlans.handle().stream()
                .map(SubscriptionResourceAssembler::toResource)
                .toList();
    }

    @GetMapping("/accounts/{accountId}/subscription")
    @Operation(summary = "Get the current subscription and enabled capabilities for an account")
    public SubscriptionResource current(@PathVariable String accountId) {
        return SubscriptionResourceAssembler.toResource(getSubscription.handle(accountId));
    }

    @PutMapping("/accounts/{accountId}/subscription")
    @Operation(summary = "Activate or change the subscription plan for an account")
    public SubscriptionResource change(
            @PathVariable String accountId,
            @Valid @RequestBody ChangeSubscriptionResource resource
    ) {
        return SubscriptionResourceAssembler.toResource(
                changeSubscription.handle(
                        SubscriptionResourceAssembler.toCommand(accountId, resource)
                )
        );
    }
}
