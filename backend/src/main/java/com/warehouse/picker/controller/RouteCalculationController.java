
package com.warehouse.picker.controller;

import com.warehouse.picker.service.RouteCalculationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.warehouse.picker.dto.ApiError;
import io.swagger.v3.oas.annotations.media.ExampleObject;

@RestController
@RequestMapping("/api/routes")
@Tag(
        name = "Route Optimization",
        description = "Calculation and comparison of warehouse picking routes"
)
public class RouteCalculationController {

    private final RouteCalculationService routeCalculationService;

    public RouteCalculationController(
            RouteCalculationService routeCalculationService
    ) {
        this.routeCalculationService = routeCalculationService;
    }

    @PostMapping(
            value = "/calculate/{taskId}",
            produces = MediaType.APPLICATION_JSON_VALUE
    )
    @Operation(
            summary = "Calculate and compare picking routes",
            description = """
                    Calculates two routes for an existing picking task.

                    OPTIMIZED: a greedy weight-aware routing heuristic.

                    S_SHAPE: a coordinate-based S-shape baseline.

                    Both algorithms use identical warehouse nodes,
                    paths, products, quantities and product weights.

                    The response contains picking stops, travel distance,
                    accumulated load-distance cost and the full path.

                    The operation calculates routes without saving
                    them to the database.
                    """
    )

    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Both routes calculated successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(
                                    implementation =
                                            RouteCalculationService.RouteComparison.class
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid route calculation parameters",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Authentication is required",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "403",
                    description = "User does not have the required role",
                    content = @Content
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Picking task not found",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class),
                            examples = @ExampleObject(
                                    name = "Task not found",
                                    value = """
                                        {
                                          "timestamp": "2026-11-23T00:00:00+02:00",
                                          "status": 404,
                                          "error": "Not Found",
                                          "message": "Picking task not found: 999999999",
                                          "path": "/api/routes/calculate/999999999"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Warehouse data or stock prevents route calculation",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = ApiError.class)
                    )
            )
    })

    public ResponseEntity<RouteCalculationService.RouteComparison>
    calculateRoutes(
            @Parameter(
                    description = "Identifier of an existing picking task",
                    required = true,
                    example = "1"
            )
            @PathVariable Long taskId
    ) {
        RouteCalculationService.RouteComparison result =
                routeCalculationService.calculateRoutes(taskId);

        return ResponseEntity.ok(result);
    }
}
