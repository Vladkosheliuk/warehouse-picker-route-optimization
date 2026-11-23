
package com.warehouse.picker.controller;

import com.warehouse.picker.dto.ApiError;
import com.warehouse.picker.dto.WarehouseDTO;
import com.warehouse.picker.entity.Warehouse;
import com.warehouse.picker.mapper.WarehouseMapper;
import com.warehouse.picker.service.WarehouseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warehouses")
@Tag(
        name = "Warehouse Management",
        description = "Create, retrieve, update and delete warehouses. " +
                "Requires the MANAGER role."
)
public class WarehouseController {

    private final WarehouseService warehouseService;
    private final WarehouseMapper warehouseMapper;

    public WarehouseController(
            WarehouseService warehouseService,
            WarehouseMapper warehouseMapper
    ) {
        this.warehouseService = warehouseService;
        this.warehouseMapper = warehouseMapper;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Get all warehouses",
            description = "Returns all configured warehouses."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Warehouses retrieved successfully",
                    content = @Content(
                            array = @ArraySchema(
                                    schema = @Schema(
                                            implementation = WarehouseDTO.class
                                    )
                            )
                    )
            ),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required"),
            @ApiResponse(responseCode = "403",
                    description = "Manager role required")
    })
    public List<WarehouseDTO> getAllWarehouses() {
        return warehouseService.getAllWarehouses()
                .stream()
                .map(warehouseMapper::toDTO)
                .toList();
    }

    @GetMapping(
            value = "/{id}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Get warehouse by ID",
            description = "Returns warehouse details for the given ID."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Warehouse found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = WarehouseDTO.class))),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required"),
            @ApiResponse(responseCode = "403",
                    description = "Manager role required"),
            @ApiResponse(responseCode = "404",
                    description = "Warehouse not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class)))
    })
    public WarehouseDTO getWarehouseById(
            @Parameter(description = "Warehouse ID", example = "1")
            @PathVariable Long id
    ) {
        Warehouse warehouse =
                warehouseService.getWarehouseById(id);

        return warehouseMapper.toDTO(warehouse);
    }

    @PostMapping(
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Create a warehouse",
            description = "Creates a warehouse with a name and " +
                    "an optional description."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201",
                    description = "Warehouse created",
                    content = @Content(
                            schema = @Schema(
                                    implementation = WarehouseDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid warehouse data",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class))),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required"),
            @ApiResponse(responseCode = "403",
                    description = "Manager role required")
    })
    public WarehouseDTO createWarehouse(
            @Valid @RequestBody WarehouseDTO dto
    ) {
        Warehouse warehouse = warehouseMapper.toEntity(dto);
        Warehouse savedWarehouse =
                warehouseService.createWarehouse(warehouse);

        return warehouseMapper.toDTO(savedWarehouse);
    }

    @PutMapping(
            value = "/{id}",
            consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Update a warehouse",
            description = "Updates the name and description " +
                    "of an existing warehouse."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200",
                    description = "Warehouse updated",
                    content = @Content(
                            schema = @Schema(
                                    implementation = WarehouseDTO.class))),
            @ApiResponse(responseCode = "400",
                    description = "Invalid warehouse data",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class))),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required"),
            @ApiResponse(responseCode = "403",
                    description = "Manager role required"),
            @ApiResponse(responseCode = "404",
                    description = "Warehouse not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class)))
    })
    public WarehouseDTO updateWarehouse(
            @Parameter(description = "Warehouse ID", example = "1")
            @PathVariable Long id,
            @Valid @RequestBody WarehouseDTO dto
    ) {
        Warehouse warehouse = warehouseMapper.toEntity(dto);

        Warehouse updatedWarehouse =
                warehouseService.updateWarehouse(id, warehouse);

        return warehouseMapper.toDTO(updatedWarehouse);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Delete a warehouse",
            description = "Deletes an existing warehouse. " +
                    "Deletion may fail if dependent records exist."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204",
                    description = "Warehouse deleted successfully"),
            @ApiResponse(responseCode = "401",
                    description = "Authentication required"),
            @ApiResponse(responseCode = "403",
                    description = "Manager role required"),
            @ApiResponse(responseCode = "404",
                    description = "Warehouse not found",
                    content = @Content(
                            schema = @Schema(
                                    implementation = ApiError.class)))
    })
    public void deleteWarehouse(
            @Parameter(description = "Warehouse ID", example = "1")
            @PathVariable Long id
    ) {
        warehouseService.deleteWarehouse(id);
    }
}
