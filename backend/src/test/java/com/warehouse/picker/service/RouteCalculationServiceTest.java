
package com.warehouse.picker.service;

import com.warehouse.picker.algorithm.RouteOptimizer;
import com.warehouse.picker.algorithm.SShapeRoutePlanner;
import com.warehouse.picker.entity.*;
import com.warehouse.picker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteCalculationServiceTest {

    @Mock
    private PickingTaskRepository taskRepository;

    @Mock
    private PickingTaskItemRepository taskItemRepository;

    @Mock
    private WarehouseNodeRepository nodeRepository;

    @Mock
    private WarehouseEdgeRepository edgeRepository;

    @Mock
    private ProductLocationRepository locationRepository;

    @Mock
    private RouteOptimizer routeOptimizer;

    @Mock
    private SShapeRoutePlanner sShapeRoutePlanner;

    @InjectMocks
    private RouteCalculationService service;

    private Warehouse warehouse;
    private PickingTask task;

    @BeforeEach
    void setUp() {
        warehouse = withId(new Warehouse(), 1L);

        task = new PickingTask();
        task.setWarehouse(warehouse);
    }

    @Test
    void shouldRejectNonExistingTask() {
        when(taskRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.calculateRoutes(999L)
        );

        verifyNoInteractions(routeOptimizer, sShapeRoutePlanner);
    }

    @Test
    void shouldRejectWarehouseWithoutEntrance() {
        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of(node(2L, "EXIT")));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.calculateRoutes(1L)
        );

        assertEquals(
                "Warehouse has no entrance",
                exception.getMessage()
        );
    }

    @Test
    void shouldRejectWarehouseWithoutExit() {
        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of(node(1L, "ENTRANCE")));

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateRoutes(1L)
        );
    }

    @Test
    void shouldRejectEmptyPickingTask() {
        givenValidWarehouse();

        when(taskItemRepository.findByTaskId(1L))
                .thenReturn(List.of());

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateRoutes(1L)
        );

        verifyNoInteractions(routeOptimizer, sShapeRoutePlanner);
    }

    @Test
    void shouldRejectInsufficientStock() {
        givenValidWarehouse();

        Product product = product(100L);

        when(taskItemRepository.findByTaskId(1L))
                .thenReturn(List.of(item(product, 5, 0)));

        when(locationRepository.findByNodeWarehouseId(1L))
                .thenReturn(List.of(
                        location(product, 3L, 2)
                ));

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.calculateRoutes(1L)
        );

        assertTrue(
                exception.getMessage().contains("Insufficient stock")
        );

        verifyNoInteractions(routeOptimizer, sShapeRoutePlanner);
    }

    @Test
    void shouldRejectTaskWithNoRemainingProducts() {
        givenValidWarehouse();

        Product product = product(100L);

        when(taskItemRepository.findByTaskId(1L))
                .thenReturn(List.of(item(product, 2, 2)));

        assertThrows(
                IllegalStateException.class,
                () -> service.calculateRoutes(1L)
        );

        verifyNoInteractions(routeOptimizer, sShapeRoutePlanner);
    }

    @Test
    void shouldAllocateStockAndCalculateBothRoutes() {
        givenValidWarehouse();

        Product product = product(100L);

        when(taskItemRepository.findByTaskId(1L))
                .thenReturn(List.of(item(product, 4, 0)));

        when(locationRepository.findByNodeWarehouseId(1L))
                .thenReturn(List.of(
                        location(product, 4L, 5),
                        location(product, 3L, 2)
                ));

        RouteOptimizer.RouteResult route =
                new RouteOptimizer.RouteResult(
                        List.of(),
                        new BigDecimal("50"),
                        new BigDecimal("100"),
                        List.of(1L, 3L, 4L, 2L)
                );

        when(routeOptimizer.optimize(
                eq(1L), eq(2L),
                anyList(), anyList(), anyList()
        )).thenReturn(route);

        when(sShapeRoutePlanner.plan(
                eq(1L), eq(2L),
                anyList(), anyList(), anyList()
        )).thenReturn(route);

        var result = service.calculateRoutes(1L);

        assertEquals(1L, result.taskId());
        assertEquals(1L, result.warehouseId());

        assertSame(route, result.optimized());
        assertSame(route, result.sShape());

        verify(routeOptimizer).optimize(
                eq(1L),
                eq(2L),
                argThat(picks ->
                        picks.size() == 2
                                && picks.get(0).nodeId().equals(3L)
                                && picks.get(0).quantity() == 2
                                && picks.get(1).nodeId().equals(4L)
                                && picks.get(1).quantity() == 2
                ),
                anyList(),
                anyList()
        );

        verify(sShapeRoutePlanner).plan(
                eq(1L), eq(2L),
                anyList(), anyList(), anyList()
        );
    }

    private void givenValidWarehouse() {
        when(taskRepository.findById(1L))
                .thenReturn(Optional.of(task));

        when(nodeRepository.findByWarehouseId(1L))
                .thenReturn(List.of(
                        node(1L, "ENTRANCE"),
                        node(2L, "EXIT"),
                        node(3L, "STORAGE"),
                        node(4L, "STORAGE")
                ));
    }

    private WarehouseNode node(Long id, String type) {
        WarehouseNode node = withId(new WarehouseNode(), id);
        node.setNodeType(type);
        node.setWarehouse(warehouse);
        return node;
    }

    private Product product(Long id) {
        Product product = withId(new Product(), id);
        product.setSku("TEST-100");
        product.setName("Test Product");
        product.setWeightKg(new BigDecimal("1.500"));
        return product;
    }

    private PickingTaskItem item(
            Product product,
            int required,
            int collected
    ) {
        PickingTaskItem item = new PickingTaskItem();
        item.setTask(task);
        item.setProduct(product);
        item.setRequiredQuantity(required);
        item.setCollectedQuantity(collected);
        return item;
    }

    private ProductLocation location(
            Product product,
            Long nodeId,
            int quantity
    ) {
        ProductLocation location = new ProductLocation();
        location.setProduct(product);
        location.setNode(node(nodeId, "STORAGE"));
        location.setQuantity(quantity);
        return location;
    }

    private <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
            return entity;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
