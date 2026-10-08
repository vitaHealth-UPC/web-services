package com.tata.inventoryreplenishment.interfaces.rest;

import com.tata.inventoryreplenishment.application.commandservices.InventoryCommandService;
import com.tata.inventoryreplenishment.application.queryservices.InventoryQueryService;
import com.tata.inventoryreplenishment.domain.model.queries.GetRemainingStockQuery;
import com.tata.inventoryreplenishment.interfaces.rest.resources.ErrorResource;
import com.tata.inventoryreplenishment.interfaces.rest.resources.InventoryResource;
import com.tata.inventoryreplenishment.interfaces.rest.resources.RegisterInitialInventoryResource;
import com.tata.inventoryreplenishment.interfaces.rest.resources.RegisterReplenishmentResource;
import com.tata.inventoryreplenishment.interfaces.rest.transform.InventoryResourceAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/inventories")
@Tag(name = "Inventory", description = "Medication stock, low stock detection and replenishment")
public class InventoryController {
    private final InventoryCommandService commandService;
    private final InventoryQueryService queryService;

    public InventoryController(InventoryCommandService commandService, InventoryQueryService queryService) {
        this.commandService = commandService;
        this.queryService = queryService;
    }

    @Operation(
            summary = "Register the initial inventory of a medication",
            description = "Creates the stock of a medication with its first batch and replenishment threshold (US-40). "
                    + "Only one inventory can exist per medication.")
    @ApiResponse(responseCode = "201", description = "Inventory registered")
    @ApiResponse(responseCode = "404", description = "Medication does not exist",
            content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    @ApiResponse(responseCode = "400", description = "Missing fields or invalid quantity",
            content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    @ApiResponse(responseCode = "409", description = "Medication is inactive or an inventory already exists for this medication",
            content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResource registerInitialInventory(@Valid @RequestBody RegisterInitialInventoryResource resource) {
        return InventoryResourceAssembler.toResource(
                commandService.registerInitialInventory(InventoryResourceAssembler.toCommand(resource))
        );
    }

    @Operation(
            summary = "Get the remaining stock of a medication",
            description = "Remaining units, threshold, low stock flag and registered batches (US-41, US-42).")
    @ApiResponse(responseCode = "200", description = "Inventory returned")
    @ApiResponse(responseCode = "404", description = "No inventory is registered for this medication",
            content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    @GetMapping("/{medicationId}")
    public InventoryResource getRemainingStock(@PathVariable String medicationId) {
        return InventoryResourceAssembler.toResource(
                queryService.getRemainingStock(new GetRemainingStockQuery(medicationId))
        );
    }

    @Operation(
            summary = "Register a replenishment",
            description = "Adds a batch to the inventory and increases the remaining stock (US-43).")
    @ApiResponse(responseCode = "201", description = "Replenishment registered; returns the updated inventory")
    @ApiResponse(responseCode = "400", description = "Missing fields or invalid quantity",
            content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    @ApiResponse(responseCode = "404", description = "No inventory is registered for this medication",
            content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    @ApiResponse(responseCode = "409", description = "The inventory was modified concurrently; retry the request",
            content = @Content(schema = @Schema(implementation = ErrorResource.class)))
    @PostMapping("/{medicationId}/replenishments")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryResource registerReplenishment(
            @PathVariable String medicationId,
            @Valid @RequestBody RegisterReplenishmentResource resource
    ) {
        return InventoryResourceAssembler.toResource(
                commandService.registerReplenishment(InventoryResourceAssembler.toCommand(medicationId, resource))
        );
    }
}
