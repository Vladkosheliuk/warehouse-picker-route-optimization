package com.warehouse.picker.algorithm;

import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ShortestPathFinderTest {

    private ShortestPathFinder finder;

    @BeforeEach
    void setUp() {
        finder = new ShortestPathFinder();
    }

    @Test
    void shouldFindShortestPath() {
        WarehouseNode a = node(1L);
        WarehouseNode b = node(2L);
        WarehouseNode c = node(3L);

        List<WarehouseEdge> edges = List.of(
                edge(a, b, "5", true),
                edge(b, c, "7", true),
                edge(a, c, "20", true)
        );

        var result = finder.findShortestPath(
                1L, 3L, List.of(a, b, c), edges
        );

        assertEquals(List.of(1L, 2L, 3L), result.nodeIds());
        assertEquals(
                0,
                new BigDecimal("12").compareTo(result.distanceM())
        );
    }

    @Test
    void shouldReturnZeroDistanceForSameNode() {
        WarehouseNode a = node(1L);

        var result = finder.findShortestPath(
                1L, 1L, List.of(a), List.of()
        );

        assertEquals(List.of(1L), result.nodeIds());
        assertEquals(
                0,
                BigDecimal.ZERO.compareTo(result.distanceM())
        );
    }

    @Test
    void shouldRespectOneWayEdges() {
        WarehouseNode a = node(1L);
        WarehouseNode b = node(2L);

        List<WarehouseEdge> edges = List.of(
                edge(a, b, "5", false)
        );

        var result = finder.findShortestPath(
                1L, 2L, List.of(a, b), edges
        );

        assertEquals(List.of(1L, 2L), result.nodeIds());

        assertThrows(
                IllegalArgumentException.class,
                () -> finder.findShortestPath(
                        2L, 1L, List.of(a, b), edges
                )
        );
    }

    @Test
    void shouldRejectUnreachableDestination() {
        WarehouseNode a = node(1L);
        WarehouseNode b = node(2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> finder.findShortestPath(
                        1L, 2L, List.of(a, b), List.of()
                )
        );
    }

    @Test
    void shouldRejectUnknownNode() {
        WarehouseNode a = node(1L);

        assertThrows(
                IllegalArgumentException.class,
                () -> finder.findShortestPath(
                        1L, 99L, List.of(a), List.of()
                )
        );
    }

    @Test
    void shouldRejectInvalidEdgeDistance() {
        WarehouseNode a = node(1L);
        WarehouseNode b = node(2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> finder.findShortestPath(
                        1L,
                        2L,
                        List.of(a, b),
                        List.of(edge(a, b, "0", true))
                )
        );
    }

    private WarehouseNode node(Long id) {
        WarehouseNode node = new WarehouseNode();

        try {
            var field = WarehouseNode.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(node, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        return node;
    }

    private WarehouseEdge edge(
            WarehouseNode from,
            WarehouseNode to,
            String distance,
            boolean bidirectional
    ) {
        WarehouseEdge edge = new WarehouseEdge();
        edge.setFromNode(from);
        edge.setToNode(to);
        edge.setDistanceM(new BigDecimal(distance));
        edge.setBidirectional(bidirectional);
        return edge;
    }
}
