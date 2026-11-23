
package com.warehouse.picker.algorithm;

import com.warehouse.picker.entity.WarehouseEdge;
import com.warehouse.picker.entity.WarehouseNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RouteOptimizerTest {

    private RouteOptimizer optimizer;

    @BeforeEach
    void setUp() {
        optimizer = new RouteOptimizer(new ShortestPathFinder());
    }

    @Test
    void shouldCalculateDistanceAndCarriedWeight() {
        WarehouseNode entrance = node(1L);
        WarehouseNode storage = node(2L);
        WarehouseNode exit = node(3L);

        List<WarehouseEdge> edges = List.of(
                edge(entrance, storage, "10"),
                edge(storage, exit, "5")
        );

        var result = optimizer.optimize(
                1L,
                3L,
                List.of(
                        new RouteOptimizer.PickLocation(
                                2L, 100L, 2, new BigDecimal("3")
                        )
                ),
                List.of(entrance, storage, exit),
                edges
        );

        assertDecimalEquals("15", result.totalDistanceM());
        assertDecimalEquals("30", result.loadDistanceKgM());
        assertEquals(List.of(1L, 2L, 3L),
                result.fullPathNodeIds());

        assertEquals(2, result.stops().size());
        assertDecimalEquals("6",
                result.stops().get(0).carriedWeightAfterKg());
        assertDecimalEquals("6",
                result.stops().get(1).carriedWeightAfterKg());
    }

    @Test
    void shouldPreferLighterItemAtEqualDistance() {
        WarehouseNode entrance = node(1L);
        WarehouseNode light = node(2L);
        WarehouseNode heavy = node(3L);

        List<WarehouseEdge> edges = List.of(
                edge(entrance, light, "5"),
                edge(entrance, heavy, "5"),
                edge(light, heavy, "5")
        );

        var result = optimizer.optimize(
                1L,
                1L,
                List.of(
                        new RouteOptimizer.PickLocation(
                                3L, 200L, 1, new BigDecimal("10")
                        ),
                        new RouteOptimizer.PickLocation(
                                2L, 100L, 1, new BigDecimal("1")
                        )
                ),
                List.of(entrance, light, heavy),
                edges
        );

        assertEquals(100L, result.stops().get(0).productId());
        assertEquals(200L, result.stops().get(1).productId());
    }

    @Test
    void shouldHandleEmptyPickingList() {
        WarehouseNode entrance = node(1L);
        WarehouseNode exit = node(2L);

        var result = optimizer.optimize(
                1L,
                2L,
                List.of(),
                List.of(entrance, exit),
                List.of(edge(entrance, exit, "8"))
        );

        assertDecimalEquals("8", result.totalDistanceM());
        assertDecimalEquals("0", result.loadDistanceKgM());
        assertEquals(1, result.stops().size());
        assertNull(result.stops().get(0).productId());
    }

    @Test
    void shouldRejectUnreachablePickingLocation() {
        WarehouseNode entrance = node(1L);
        WarehouseNode storage = node(2L);

        assertThrows(
                IllegalArgumentException.class,
                () -> optimizer.optimize(
                        1L,
                        1L,
                        List.of(
                                new RouteOptimizer.PickLocation(
                                        2L, 100L, 1,
                                        new BigDecimal("2")
                                )
                        ),
                        List.of(entrance, storage),
                        List.of()
                )
        );
    }

    @Test
    void shouldRejectInvalidPickQuantity() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RouteOptimizer.PickLocation(
                        2L, 100L, 0, new BigDecimal("2")
                )
        );
    }

    @Test
    void shouldRejectInvalidProductWeight() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new RouteOptimizer.PickLocation(
                        2L, 100L, 1, BigDecimal.ZERO
                )
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

    private WarehouseNode node(Long id) {
        WarehouseNode node = new WarehouseNode();

        try {
            Field field = WarehouseNode.class.getDeclaredField("id");
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
