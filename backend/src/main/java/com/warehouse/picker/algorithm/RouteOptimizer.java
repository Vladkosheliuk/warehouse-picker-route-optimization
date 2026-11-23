
package com.warehouse.picker.algorithm;

import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class RouteOptimizer {

    private final ShortestPathFinder pathFinder;

    public RouteOptimizer(ShortestPathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    public record PickLocation(
            Long nodeId,
            Long productId,
            int quantity,
            BigDecimal unitWeightKg
    ) {
        public PickLocation {
            Objects.requireNonNull(nodeId);
            Objects.requireNonNull(productId);
            Objects.requireNonNull(unitWeightKg);

            if (quantity <= 0 ||
                    unitWeightKg.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Quantity and product weight must be positive"
                );
            }
        }

        public BigDecimal totalWeightKg() {
            return unitWeightKg.multiply(
                    BigDecimal.valueOf(quantity)
            );
        }
    }

    public record RouteStop(
            Long nodeId,
            Long productId,
            int quantity,
            BigDecimal distanceFromPreviousM,
            BigDecimal carriedWeightAfterKg
    ) {
    }

    public record RouteResult(
            List<RouteStop> stops,
            BigDecimal totalDistanceM,
            BigDecimal loadDistanceKgM,
            List<Long> fullPathNodeIds
    ) {
    }

    public RouteResult optimize(
            Long startNodeId,
            Long endNodeId,
            List<PickLocation> pickLocations,
            List<WarehouseNode> nodes,
            List<WarehouseEdge> edges
    ) {
        Objects.requireNonNull(startNodeId);
        Objects.requireNonNull(endNodeId);
        Objects.requireNonNull(pickLocations);
        Objects.requireNonNull(nodes);
        Objects.requireNonNull(edges);

        List<PickLocation> remaining =
                new ArrayList<>(pickLocations);

        List<RouteStop> stops = new ArrayList<>();
        List<Long> fullPath = new ArrayList<>();

        Long currentNodeId = startNodeId;

        BigDecimal totalDistance = BigDecimal.ZERO;
        BigDecimal loadDistance = BigDecimal.ZERO;
        BigDecimal carriedWeight = BigDecimal.ZERO;

        fullPath.add(startNodeId);

        while (!remaining.isEmpty()) {

            PickLocation bestLocation = null;
            ShortestPathFinder.PathResult bestPath = null;
            BigDecimal bestScore = null;

            for (PickLocation candidate : remaining) {

                ShortestPathFinder.PathResult path;

                try {
                    path = pathFinder.findShortestPath(
                            currentNodeId,
                            candidate.nodeId(),
                            nodes,
                            edges
                    );
                } catch (IllegalArgumentException e) {
                    continue;
                }

                BigDecimal distance = path.distanceM();

                // Prefer shorter travel and lighter early picks.
                BigDecimal score = distance.add(
                        candidate.totalWeightKg()
                                .multiply(new BigDecimal("0.1"))
                );

                if (bestScore == null ||
                        score.compareTo(bestScore) < 0) {
                    bestScore = score;
                    bestLocation = candidate;
                    bestPath = path;
                }
            }

            if (bestLocation == null || bestPath == null) {
                throw new IllegalArgumentException(
                        "Some picking locations are unreachable"
                );
            }

            BigDecimal segmentDistance = bestPath.distanceM();

            // Weight is carried before collecting the next product.
            loadDistance = loadDistance.add(
                    segmentDistance.multiply(carriedWeight)
            );

            totalDistance = totalDistance.add(segmentDistance);

            appendPath(fullPath, bestPath.nodeIds());

            carriedWeight = carriedWeight.add(
                    bestLocation.totalWeightKg()
            );

            stops.add(new RouteStop(
                    bestLocation.nodeId(),
                    bestLocation.productId(),
                    bestLocation.quantity(),
                    segmentDistance,
                    carriedWeight
            ));

            currentNodeId = bestLocation.nodeId();
            remaining.remove(bestLocation);
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

        stops.add(new RouteStop(
                endNodeId,
                null,
                0,
                exitDistance,
                carriedWeight
        ));

        return new RouteResult(
                List.copyOf(stops),
                totalDistance,
                loadDistance,
                List.copyOf(fullPath)
        );
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
