
package com.warehouse.picker.service;

import com.warehouse.picker.algorithm.RouteOptimizer;
import com.warehouse.picker.algorithm.SShapeRoutePlanner;
import com.warehouse.picker.entity.*;
import com.warehouse.picker.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

@Service
public class RouteCalculationService {

    private final PickingTaskRepository taskRepository;
    private final PickingTaskItemRepository taskItemRepository;
    private final WarehouseNodeRepository nodeRepository;
    private final WarehouseEdgeRepository edgeRepository;
    private final ProductLocationRepository locationRepository;
    private final RouteOptimizer routeOptimizer;
    private final SShapeRoutePlanner sShapeRoutePlanner;

    public RouteCalculationService(
            PickingTaskRepository taskRepository,
            PickingTaskItemRepository taskItemRepository,
            WarehouseNodeRepository nodeRepository,
            WarehouseEdgeRepository edgeRepository,
            ProductLocationRepository locationRepository,
            RouteOptimizer routeOptimizer,
            SShapeRoutePlanner sShapeRoutePlanner
    ) {
        this.taskRepository = taskRepository;
        this.taskItemRepository = taskItemRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
        this.locationRepository = locationRepository;
        this.routeOptimizer = routeOptimizer;
        this.sShapeRoutePlanner = sShapeRoutePlanner;
    }

    @Transactional(readOnly = true)
    public RouteComparison calculateRoutes(Long taskId) {


        PickingTask task = taskRepository.findById(taskId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Picking task not found: " + taskId
                        )
                );


        Long warehouseId = task.getWarehouse().getId();

        List<WarehouseNode> nodes =
                nodeRepository.findByWarehouseId(warehouseId);

        List<WarehouseEdge> edges =
                edgeRepository.findByWarehouseId(warehouseId);

        WarehouseNode entrance = nodes.stream()
                .filter(node -> "ENTRANCE".equals(node.getNodeType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Warehouse has no entrance"
                        )
                );

        WarehouseNode exit = nodes.stream()
                .filter(node -> "EXIT".equals(node.getNodeType()))
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Warehouse has no exit"
                        )
                );

        List<PickingTaskItem> taskItems =
                taskItemRepository.findByTaskId(taskId);

        if (taskItems.isEmpty()) {
            throw new IllegalStateException(
                    "Picking task contains no items"
            );
        }

        List<ProductLocation> locations =
                locationRepository.findByNodeWarehouseId(warehouseId);

        List<RouteOptimizer.PickLocation> pickLocations =
                new ArrayList<>();

        for (PickingTaskItem item : taskItems) {

            int remainingQuantity =
                    item.getRequiredQuantity()
                            - item.getCollectedQuantity();

            if (remainingQuantity <= 0) {
                continue;
            }

            Product product = item.getProduct();

            List<ProductLocation> availableLocations =
                    locations.stream()
                            .filter(location ->
                                    location.getProduct()
                                            .getId()
                                            .equals(product.getId()))
                            .filter(location ->
                                    location.getQuantity() > 0)
                            .sorted(Comparator.comparing(
                                    location ->
                                            location.getNode().getId()
                            ))
                            .toList();

            for (ProductLocation location : availableLocations) {

                if (remainingQuantity == 0) {
                    break;
                }

                int quantityToPick = Math.min(
                        remainingQuantity,
                        location.getQuantity()
                );

                pickLocations.add(
                        new RouteOptimizer.PickLocation(
                                location.getNode().getId(),
                                product.getId(),
                                quantityToPick,
                                product.getWeightKg()
                        )
                );

                remainingQuantity -= quantityToPick;
            }

            if (remainingQuantity > 0) {
                throw new IllegalStateException(
                        "Insufficient stock for product: "
                                + product.getSku()
                );
            }
        }

        if (pickLocations.isEmpty()) {
            throw new IllegalStateException(
                    "No remaining products to pick"
            );
        }

        RouteOptimizer.RouteResult optimized =
                routeOptimizer.optimize(
                        entrance.getId(),
                        exit.getId(),
                        pickLocations,
                        nodes,
                        edges
                );

        RouteOptimizer.RouteResult sShape =
                sShapeRoutePlanner.plan(
                        entrance.getId(),
                        exit.getId(),
                        pickLocations,
                        nodes,
                        edges
                );

        return new RouteComparison(
                taskId,
                warehouseId,
                optimized,
                sShape
        );
    }

    public record RouteComparison(
            Long taskId,
            Long warehouseId,
            RouteOptimizer.RouteResult optimized,
            RouteOptimizer.RouteResult sShape
    ) {
    }
}
