
package com.warehouse.picker.algorithm;

import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SShapeRoutePlannerTest {

    private SShapeRoutePlanner planner;

    @BeforeEach
    void setUp() {
        planner = new SShapeRoutePlanner(new ShortestPathFinder());
    }

    @Test
    void shouldVisitStorageLocationsInSnakeOrder() {
        WarehouseNode start = node(1L, "0", "0");
        WarehouseNode a = node(2L, "0", "10");
        WarehouseNode b = node(3L, "0", "20");
        WarehouseNode c = node(4L, "10", "20");
        WarehouseNode d = node(5L, "10", "10");
        WarehouseNode exit = node(6L, "10", "0");

        List<WarehouseNode> nodes =
                List.of(start, a, b, c, d, exit);

        List<WarehouseEdge> edges = List.of(
                edge(start, a, "10"),
                edge(a, b, "10"),
                edge(b, c, "10"),
                edge(c, d, "10"),
                edge(d, exit, "10")
        );

        var result = planner.plan(
                1L,
                6L,
                List.of(
                        pick(5L, 104L, "1"),
                        pick(3L, 102L, "1"),
                        pick(4L, 103L, "1"),
                        pick(2L, 101L, "1")
                ),
                nodes,
                edges
        );

        assertEquals(
                List.of(2L, 3L, 4L, 5L, 6L),
                result.stops().stream()
                        .map(RouteOptimizer.RouteStop::nodeId)
                        .toList()
        );

        assertDecimalEquals("50", result.totalDistanceM());
        assertDecimalEquals("100", result.loadDistanceKgM());
    }

    @Test
    void shouldHandleEmptyPickingList() {
        WarehouseNode start = node(1L, "0", "0");
        WarehouseNode exit = node(2L, "10", "0");

        var result = planner.plan(
                1L,
                2L,
                List.of(),
                List.of(start, exit),
                List.of(edge(start, exit, "10"))
        );

        assertDecimalEquals("10", result.totalDistanceM());
        assertDecimalEquals("0", result.loadDistanceKgM());
        assertEquals(1, result.stops().size());
    }

    @Test
    void shouldRejectMissingPickingLocation() {
        WarehouseNode start = node(1L, "0", "0");

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.plan(
                        1L,
                        1L,
                        List.of(pick(99L, 100L, "2")),
                        List.of(start),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectUnreachableStorage() {
        WarehouseNode start = node(1L, "0", "0");
        WarehouseNode storage = node(2L, "0", "10");

        assertThrows(
                IllegalArgumentException.class,
                () -> planner.plan(
                        1L,
                        1L,
                        List.of(pick(2L, 100L, "2")),
                        List.of(start, storage),
                        List.of()
                )
        );
    }

    @Test
    void shouldCalculateLoadDistance() {
        WarehouseNode start = node(1L, "0", "0");
        WarehouseNode storage = node(2L, "0", "10");
        WarehouseNode exit = node(3L, "0", "20");

        var result = planner.plan(
                1L,
                3L,
                List.of(pick(2L, 100L, "3")),
                List.of(start, storage, exit),
                List.of(
                        edge(start, storage, "10"),
                        edge(storage, exit, "10")
                )
        );

        assertDecimalEquals("20", result.totalDistanceM());
        assertDecimalEquals("30", result.loadDistanceKgM());
    }

    private RouteOptimizer.PickLocation pick(
            Long nodeId,
            Long productId,
            String weight
    ) {
        return new RouteOptimizer.PickLocation(
                nodeId,
                productId,
                1,
                new BigDecimal(weight)
        );
    }

    private void assertDecimalEquals(
            String expected,
            BigDecimal actual
    ) {
        assertEquals(
                0,
                new BigDecimal(expected).compareTo(actual)
        );
    }

    private WarehouseNode node(
            Long id,
            String x,
            String y
    ) {
        WarehouseNode node = new WarehouseNode();

        try {
            Field field =
                    WarehouseNode.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(node, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }

        node.setXCoordinate(new BigDecimal(x));
        node.setYCoordinate(new BigDecimal(y));

        return node;
    }

    private WarehouseEdge edge(
            WarehouseNode from,
            WarehouseNode to,
            String distance
    ) {
        WarehouseEdge edge = new WarehouseEdge();
        edge.setFromNode(from);
        edge.setToNode(to);
        edge.setDistanceM(new BigDecimal(distance));
        edge.setBidirectional(true);
        return edge;
    }
}
