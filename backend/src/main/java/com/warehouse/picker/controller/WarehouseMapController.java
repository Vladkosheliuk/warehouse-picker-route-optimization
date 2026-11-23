
package com.warehouse.picker.controller;

import com.warehouse.picker.dto.WarehouseMapDTO;
import com.warehouse.picker.service.WarehouseMapService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouses/{warehouseId}/map")
@Tag(
        name = "Warehouse Maps",
        description = "Import and retrieve warehouse graph layouts."
)
public class WarehouseMapController {

    private final WarehouseMapService mapService;

    public WarehouseMapController(WarehouseMapService mapService) {
        this.mapService = mapService;
    }

    @PostMapping("/import")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Import a warehouse map",
            description = "Imports a validated map into an empty warehouse. " +
                    "Requires the MANAGER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "Map imported successfully"),
            @ApiResponse(responseCode = "400",
                    description = "Invalid map"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required"),
            @ApiResponse(responseCode = "403",
                    description = "Manager role required"),
            @ApiResponse(responseCode = "404",
                    description = "Warehouse not found"),
            @ApiResponse(responseCode = "409",
                    description = "Warehouse already contains a map")
    })
    public WarehouseMapDTO importMap(
            @PathVariable Long warehouseId,
            @Valid @RequestBody WarehouseMapDTO request
    ) {
        return mapService.importMap(warehouseId, request);
    }

    @GetMapping
    @Operation(
            summary = "Get a warehouse map",
            description = "Returns the stored nodes and edges as JSON. " +
                    "Requires the MANAGER role."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Warehouse map retrieved"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required"),
            @ApiResponse(responseCode = "403",
                    description = "Manager role required"),
            @ApiResponse(responseCode = "404",
                    description = "Warehouse not found")
    })
    public WarehouseMapDTO getMap(
            @PathVariable Long warehouseId
    ) {
        return mapService.getMap(warehouseId);
    }
}
