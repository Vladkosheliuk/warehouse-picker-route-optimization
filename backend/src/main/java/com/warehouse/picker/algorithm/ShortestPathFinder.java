
package com.warehouse.picker.algorithm;

import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;

@Component
public class ShortestPathFinder {

    public record PathResult(
            List<Long> nodeIds,
            BigDecimal distanceM
    ) {
    }

    private record Connection(
            Long nodeId,
            BigDecimal distance
    ) {
    }

    private record QueueEntry(
            Long nodeId,
            BigDecimal distance
    ) {
    }

    public PathResult findShortestPath(
            Long startNodeId,
            Long endNodeId,
            List<WarehouseNode> nodes,
            List<WarehouseEdge> edges
    ) {

        Objects.requireNonNull(startNodeId);
        Objects.requireNonNull(endNodeId);
        Objects.requireNonNull(nodes);
        Objects.requireNonNull(edges);

        Map<Long, List<Connection>> graph = new HashMap<>();

        for (WarehouseNode node : nodes) {
            graph.put(node.getId(), new ArrayList<>());
        }

        if (!graph.containsKey(startNodeId)
                || !graph.containsKey(endNodeId)) {
            throw new IllegalArgumentException(
                    "Start or end node does not exist"
            );
        }

        for (WarehouseEdge edge : edges) {

            Long from = edge.getFromNode().getId();
            Long to = edge.getToNode().getId();
            BigDecimal distance = edge.getDistanceM();

            if (!graph.containsKey(from)
                    || !graph.containsKey(to)) {
                throw new IllegalArgumentException(
                        "Edge references an unknown node"
                );
            }

            if (distance == null
                    || distance.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException(
                        "Edge distance must be positive"
                );
            }

            graph.get(from).add(
                    new Connection(to, distance)
            );

            if (edge.isBidirectional()) {
                graph.get(to).add(
                        new Connection(from, distance)
                );
            }
        }

        Map<Long, BigDecimal> distances = new HashMap<>();
        Map<Long, Long> previous = new HashMap<>();

        PriorityQueue<QueueEntry> queue =
                new PriorityQueue<>(
                        Comparator.comparing(QueueEntry::distance)
                );

        distances.put(startNodeId, BigDecimal.ZERO);
        queue.add(
                new QueueEntry(startNodeId, BigDecimal.ZERO)
        );

        while (!queue.isEmpty()) {

            QueueEntry current = queue.poll();

            BigDecimal bestKnown =
                    distances.get(current.nodeId());

            if (current.distance().compareTo(bestKnown) > 0) {
                continue;
            }

            if (current.nodeId().equals(endNodeId)) {
                break;
            }

            for (Connection connection :
                    graph.get(current.nodeId())) {

                BigDecimal newDistance =
                        current.distance().add(
                                connection.distance()
                        );

                BigDecimal oldDistance =
                        distances.get(connection.nodeId());

                if (oldDistance == null
                        || newDistance.compareTo(oldDistance) < 0) {

                    distances.put(
                            connection.nodeId(),
                            newDistance
                    );

                    previous.put(
                            connection.nodeId(),
                            current.nodeId()
                    );

                    queue.add(
                            new QueueEntry(
                                    connection.nodeId(),
                                    newDistance
                            )
                    );
                }
            }
        }

        if (!distances.containsKey(endNodeId)) {
            throw new IllegalArgumentException(
                    "No path exists between the selected nodes"
            );
        }

        LinkedList<Long> path = new LinkedList<>();

        Long currentNode = endNodeId;

        while (currentNode != null) {
            path.addFirst(currentNode);
            currentNode = previous.get(currentNode);
        }

        return new PathResult(
                List.copyOf(path),
                distances.get(endNodeId)
        );
    }
}
