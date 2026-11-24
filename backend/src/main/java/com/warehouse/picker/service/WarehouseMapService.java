package com.warehouse.picker.service;

import com.warehouse.picker.dto.WarehouseMapDTO;
import com.warehouse.picker.entity.Warehouse;
import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import com.warehouse.picker.repository.WarehouseEdgeRepository;
import com.warehouse.picker.repository.WarehouseNodeRepository;
import com.warehouse.picker.repository.WarehouseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class WarehouseMapService {

    private final WarehouseRepository warehouseRepository;
    private final WarehouseNodeRepository nodeRepository;
    private final WarehouseEdgeRepository edgeRepository;

    public WarehouseMapService(
            WarehouseRepository warehouseRepository,
            WarehouseNodeRepository nodeRepository,
            WarehouseEdgeRepository edgeRepository
    ) {
        this.warehouseRepository = warehouseRepository;
        this.nodeRepository = nodeRepository;
        this.edgeRepository = edgeRepository;
    }

    @Transactional
    public WarehouseMapDTO importMap(
            Long warehouseId,
            WarehouseMapDTO map
    ) {
        Warehouse warehouse = warehouseRepository
                .findByIdForUpdate(warehouseId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Warehouse not found: " + warehouseId
                        )
                );

        if (!nodeRepository.findByWarehouseId(warehouseId).isEmpty()
                || !edgeRepository.findByWarehouseId(warehouseId).isEmpty()) {
            throw new IllegalStateException(
                    "Warehouse already contains a map. " +
                            "Import is allowed only into an empty warehouse."
            );
        }

        validateGraph(map);

        Map<String, WarehouseNode> nodesByCode = new HashMap<>();
        List<WarehouseNode> nodesToSave = new ArrayList<>();

        for (WarehouseMapDTO.NodeDTO input : map.nodes()) {
            WarehouseNode node = new WarehouseNode();

            node.setWarehouse(warehouse);
            node.setNodeCode(input.code());
            node.setNodeType(input.type().name());
            node.setXCoordinate(input.x());
            node.setYCoordinate(input.y());

            nodesByCode.put(input.code(), node);
            nodesToSave.add(node);
        }

        nodeRepository.saveAllAndFlush(nodesToSave);

        List<WarehouseEdge> edgesToSave = new ArrayList<>();

        for (WarehouseMapDTO.EdgeDTO input : map.edges()) {
            WarehouseEdge edge = new WarehouseEdge();

            edge.setWarehouse(warehouse);
            edge.setFromNode(nodesByCode.get(input.from()));
            edge.setToNode(nodesByCode.get(input.to()));
            edge.setDistanceM(input.distanceM());
            edge.setBidirectional(input.bidirectional());

            edgesToSave.add(edge);
        }

        edgeRepository.saveAllAndFlush(edgesToSave);

        return map;
    }

    @Transactional(readOnly = true)
    public WarehouseMapDTO getMap(Long warehouseId) {
        if (!warehouseRepository.existsById(warehouseId)) {
            throw new NoSuchElementException(
                    "Warehouse not found: " + warehouseId
            );
        }

        List<WarehouseMapDTO.NodeDTO> nodes =
                nodeRepository.findByWarehouseId(warehouseId)
                        .stream()
                        .map(node -> new WarehouseMapDTO.NodeDTO(
                                node.getNodeCode(),
                                WarehouseMapDTO.NodeType.valueOf(
                                        node.getNodeType()
                                ),
                                node.getXCoordinate(),
                                node.getYCoordinate()
                        ))
                        .toList();

        List<WarehouseMapDTO.EdgeDTO> edges =
                edgeRepository.findByWarehouseId(warehouseId)
                        .stream()
                        .map(edge -> new WarehouseMapDTO.EdgeDTO(
                                edge.getFromNode().getNodeCode(),
                                edge.getToNode().getNodeCode(),
                                edge.getDistanceM(),
                                edge.isBidirectional()
                        ))
                        .toList();

        return new WarehouseMapDTO(nodes, edges);
    }

    private void validateGraph(WarehouseMapDTO map) {
        Set<String> codes = new HashSet<>();

        String entrance = null;
        String exit = null;
        int entranceCount = 0;
        int exitCount = 0;
        int storageCount = 0;

        for (WarehouseMapDTO.NodeDTO node : map.nodes()) {
            if (!codes.add(node.code())) {
                throw new IllegalArgumentException(
                        "Duplicate node code: " + node.code()
                );
            }

            switch (node.type()) {
                case ENTRANCE -> {
                    entrance = node.code();
                    entranceCount++;
                }
                case EXIT -> {
                    exit = node.code();
                    exitCount++;
                }
                case STORAGE -> storageCount++;
                case INTERSECTION -> {
                    // Intersections are allowed.
                }
            }
        }

        if (entranceCount != 1 || exitCount != 1) {
            throw new IllegalArgumentException(
                    "Map must contain exactly one ENTRANCE " +
                            "and exactly one EXIT."
            );
        }

        if (storageCount == 0) {
            throw new IllegalArgumentException(
                    "Map must contain at least one STORAGE node."
            );
        }

        Map<String, Set<String>> forward = new HashMap<>();
        Map<String, Set<String>> reverse = new HashMap<>();

        for (String code : codes) {
            forward.put(code, new HashSet<>());
            reverse.put(code, new HashSet<>());
        }

        Set<String> edgeKeys = new HashSet<>();

        for (WarehouseMapDTO.EdgeDTO edge : map.edges()) {
            if (!codes.contains(edge.from())
                    || !codes.contains(edge.to())) {
                throw new IllegalArgumentException(
                        "Edge references an unknown node: " +
                                edge.from() + " -> " + edge.to()
                );
            }

            if (edge.from().equals(edge.to())) {
                throw new IllegalArgumentException(
                        "An edge cannot connect a node to itself."
                );
            }

            String key = edge.from() + "\u0000" + edge.to();

            if (!edgeKeys.add(key)) {
                throw new IllegalArgumentException(
                        "Duplicate edge: " +
                                edge.from() + " -> " + edge.to()
                );
            }

            forward.get(edge.from()).add(edge.to());
            reverse.get(edge.to()).add(edge.from());

            if (edge.bidirectional()) {
                forward.get(edge.to()).add(edge.from());
                reverse.get(edge.from()).add(edge.to());
            }
        }

        Set<String> reachableFromEntrance =
                reachable(entrance, forward);

        Set<String> ableToReachExit =
                reachable(exit, reverse);

        if (!reachableFromEntrance.containsAll(codes)
                || !ableToReachExit.containsAll(codes)) {
            throw new IllegalArgumentException(
                    "Every node must be reachable from the entrance " +
                            "and must have a path to the exit."
            );
        }
    }

    private Set<String> reachable(
            String start,
            Map<String, Set<String>> graph
    ) {
        Set<String> visited = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>();

        queue.add(start);

        while (!queue.isEmpty()) {
            String current = queue.removeFirst();

            if (!visited.add(current)) {
                continue;
            }

            for (String next : graph.get(current)) {
                if (!visited.contains(next)) {
                    queue.addLast(next);
                }
            }
        }

        return visited;
    }
}
