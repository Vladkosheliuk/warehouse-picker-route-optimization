
package com.warehouse.picker.algorithm;

import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class SShapeRoutePlanner {

    private final ShortestPathFinder pathFinder;

    public SShapeRoutePlanner(ShortestPathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    public RouteOptimizer.RouteResult plan(
            Long startNodeId,
            Long endNodeId,
            List<RouteOptimizer.PickLocation> pickLocations,
            List<WarehouseNode> nodes,
            List<WarehouseEdge> edges
    ) {
        Objects.requireNonNull(startNodeId);
        Objects.requireNonNull(endNodeId);
        Objects.requireNonNull(pickLocations);
        Objects.requireNonNull(nodes);
        Objects.requireNonNull(edges);

        Map<Long, WarehouseNode> nodeMap = new HashMap<>();

        for (WarehouseNode node : nodes) {
            if (node.getId() == null) {
                throw new IllegalArgumentException(
                        "Warehouse node ID cannot be null"
                );
            }
            nodeMap.put(node.getId(), node);
        }

        if (!nodeMap.containsKey(startNodeId)
                || !nodeMap.containsKey(endNodeId)) {
            throw new IllegalArgumentException(
                    "Start or end node does not exist"
            );
        }

        // Group picking locations by their X coordinate.
        // Each group represents a coordinate-based aisle candidate.
        TreeMap<BigDecimal, List<RouteOptimizer.PickLocation>>
                aisleGroups = new TreeMap<>();

        for (RouteOptimizer.PickLocation pick : pickLocations) {
            WarehouseNode node = nodeMap.get(pick.nodeId());

            if (node == null) {
                throw new IllegalArgumentException(
                        "Picking location does not exist: "
                                + pick.nodeId()
                );
            }

            if (node.getXCoordinate() == null
                    || node.getYCoordinate() == null) {
                throw new IllegalArgumentException(
                        "Picking location coordinates are missing"
                );
            }

            aisleGroups.computeIfAbsent(
                    node.getXCoordinate(),
                    key -> new ArrayList<>()
            ).add(pick);
        }

        // Choose initial vertical direction using the entrance.
        WarehouseNode startNode = nodeMap.get(startNodeId);

        BigDecimal startY = startNode.getYCoordinate();

        if (startY == null) {
            throw new IllegalArgumentException(
                    "Start node coordinates are missing"
            );
        }

        List<RouteOptimizer.PickLocation> orderedPicks =
                new ArrayList<>();

        int aisleIndex = 0;

        for (List<RouteOptimizer.PickLocation> aislePicks
                : aisleGroups.values()) {

            boolean ascendingY = aisleIndex % 2 == 0;

            Comparator<RouteOptimizer.PickLocation> comparator =
                    Comparator.comparing(
                            pick -> nodeMap.get(
                                    pick.nodeId()
                            ).getYCoordinate()
                    );

            if (!ascendingY) {
                comparator = comparator.reversed();
            }

            aislePicks.sort(comparator);
            orderedPicks.addAll(aislePicks);

            aisleIndex++;
        }

        // Reverse the initial traversal direction when
        // the entrance is closer to the upper aisle end.
        if (!orderedPicks.isEmpty()) {
            BigDecimal minY = orderedPicks.stream()
                    .map(p -> nodeMap.get(
                            p.nodeId()
                    ).getYCoordinate())
                    .min(BigDecimal::compareTo)
                    .orElse(startY);

            BigDecimal maxY = orderedPicks.stream()
                    .map(p -> nodeMap.get(
                            p.nodeId()
                    ).getYCoordinate())
                    .max(BigDecimal::compareTo)
                    .orElse(startY);

            BigDecimal distanceToMin =
                    startY.subtract(minY).abs();

            BigDecimal distanceToMax =
                    startY.subtract(maxY).abs();

            if (distanceToMax.compareTo(distanceToMin) < 0) {
                orderedPicks = reverseAisleDirections(
                        aisleGroups,
                        nodeMap
                );
            }
        }

        List<RouteOptimizer.RouteStop> stops =
                new ArrayList<>();

        List<Long> fullPath = new ArrayList<>();
        fullPath.add(startNodeId);

        Long currentNodeId = startNodeId;

        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal loadDistance = BigDecimal.ZERO;
        BigDecimal carriedWeight = BigDecimal.ZERO;

        for (RouteOptimizer.PickLocation pick : orderedPicks) {

            ShortestPathFinder.PathResult path =
                    pathFinder.findShortestPath(
                            currentNodeId,
                            pick.nodeId(),
                            nodes,
                            edges
                    );

            BigDecimal distance = path.distanceM();

            totalDistance = totalDistance.add(distance);

            // Weight carried before collecting this item.
            loadDistance = loadDistance.add(
                    distance.multiply(carriedWeight)
            );

            appendPath(fullPath, path.nodeIds());

            carriedWeight = carriedWeight.add(
                    pick.totalWeightKg()
            );

            stops.add(new RouteOptimizer.RouteStop(
                    pick.nodeId(),
                    pick.productId(),
                    pick.quantity(),
                    distance,
                    carriedWeight
            ));

            currentNodeId = pick.nodeId();
        }

        ShortestPathFinder.PathResult exitPath =
                pathFinder.findShortestPath(
                        currentNodeId,
                        endNodeId,
                        nodes,
                        edges
                );

        BigDecimal exitDistance = exitPath.distanceM();

        totalDistance = totalDistance.add(exitDistance);

        loadDistance = loadDistance.add(
                exitDistance.multiply(carriedWeight)
        );

        appendPath(fullPath, exitPath.nodeIds());

        stops.add(new RouteOptimizer.RouteStop(
                endNodeId,
                null,
                0,
                exitDistance,
                carriedWeight
        ));

        return new RouteOptimizer.RouteResult(
                List.copyOf(stops),
                totalDistance,
                loadDistance,
                List.copyOf(fullPath)
        );
    }

    private List<RouteOptimizer.PickLocation>
    reverseAisleDirections(
            TreeMap<BigDecimal,
                    List<RouteOptimizer.PickLocation>> aisleGroups,
            Map<Long, WarehouseNode> nodeMap
    ) {
        List<RouteOptimizer.PickLocation> result =
                new ArrayList<>();

        int aisleIndex = 0;

        for (List<RouteOptimizer.PickLocation> aislePicks
                : aisleGroups.values()) {

            List<RouteOptimizer.PickLocation> sorted =
                    new ArrayList<>(aislePicks);

            Comparator<RouteOptimizer.PickLocation> comparator =
                    Comparator.comparing(
                            pick -> nodeMap.get(
                                    pick.nodeId()
                            ).getYCoordinate()
                    );

            // First aisle: descending Y.
            if (aisleIndex % 2 == 0) {
                comparator = comparator.reversed();
            }

            sorted.sort(comparator);
            result.addAll(sorted);

            aisleIndex++;
        }

        return result;
    }

    private void appendPath(
            List<Long> fullPath,
            List<Long> segment
    ) {
        for (int i = 1; i < segment.size(); i++) {
            fullPath.add(segment.get(i));
        }
    }
}
