
package com.warehouse.picker.service;

import com.warehouse.picker.dto.PickingTaskDto.*;
import com.warehouse.picker.entity.*;
import com.warehouse.picker.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PickingTaskServiceTest {

    @Mock
    private PickingTaskRepository tasks;

    @Mock
    private PickingTaskItemRepository taskItems;

    @Mock
    private WarehouseRepository warehouses;

    @Mock
    private UserRepository users;

    @Mock
    private ProductRepository products;

    @Mock
    private ProductLocationRepository locations;

    @InjectMocks
    private PickingTaskService service;

    private User manager;
    private User picker;
    private Warehouse warehouse;
    private PickingTask task;

    @BeforeEach
    void setUp() {
        manager = user(1L, "manager", "MANAGER");
        picker = user(7L, "picker", "PICKER");

        warehouse = withId(new Warehouse(), 1L);
        warehouse.setName("Demo Warehouse");

        task = withId(new PickingTask(), 5L);
        task.setWarehouse(warehouse);
        task.setAssignedPicker(picker);
        task.setCreatedBy(manager);
        task.setStatus("ASSIGNED");
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.listAll(null)
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectUnknownAccount() {
        when(users.findByUsername("unknown"))
                .thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.listAll(auth("unknown"))
        );

        assertEquals(
                HttpStatus.UNAUTHORIZED,
                exception.getStatusCode()
        );
    }

    @Test
    void managerShouldListTasksInDescendingOrder() {
        givenManager();

        PickingTask newer = withId(new PickingTask(), 6L);
        newer.setWarehouse(warehouse);
        newer.setAssignedPicker(picker);

        when(tasks.findAll())
                .thenReturn(List.of(task, newer));

        List<TaskResponse> result =
                service.listAll(auth("manager"));

        assertEquals(2, result.size());
        assertEquals(6L, result.get(0).id());
        assertEquals(5L, result.get(1).id());
    }

    @Test
    void pickerShouldNotListAllTasks() {
        givenPicker();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.listAll(auth("picker"))
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verifyNoInteractions(tasks);
    }

    @Test
    void pickerShouldListAssignedTasks() {
        givenPicker();

        when(tasks.findByAssignedPickerId(7L))
                .thenReturn(List.of(task));

        List<TaskResponse> result =
                service.listMine(auth("picker"));

        assertEquals(1, result.size());
        assertEquals(5L, result.get(0).id());
        assertEquals("picker", result.get(0).assignedPickerUsername());
    }

    @Test
    void managerShouldNotUsePickerTaskList() {
        givenManager();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.listMine(auth("manager"))
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );
    }

    @Test
    void assignedPickerShouldAccessTask() {
        givenPicker();

        when(tasks.findById(5L))
                .thenReturn(Optional.of(task));

        TaskResponse result =
                service.findOne(5L, auth("picker"));

        assertEquals(5L, result.id());
        assertEquals("ASSIGNED", result.status());
    }

    @Test
    void otherPickerShouldNotAccessTask() {
        User other = user(8L, "other", "PICKER");

        when(users.findByUsername("other"))
                .thenReturn(Optional.of(other));

        when(tasks.findById(5L))
                .thenReturn(Optional.of(task));

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.findOne(5L, auth("other"))
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );
    }

    @Test
    void shouldRejectNonExistingTask() {
        givenManager();

        when(tasks.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> service.findOne(999L, auth("manager"))
        );
    }

    @Test
    void assignedPickerShouldStartTask() {
        givenPicker();
        givenLockedTask();

        TaskResponse result =
                service.start(5L, auth("picker"));

        assertEquals("IN_PROGRESS", result.status());
        assertNotNull(task.getStartedAt());
    }

    @Test
    void managerShouldNotStartTask() {
        givenManager();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.start(5L, auth("manager"))
        );

        assertEquals(
                HttpStatus.FORBIDDEN,
                exception.getStatusCode()
        );

        verify(tasks, never()).findByIdForUpdate(anyLong());
    }

    @Test
    void shouldNotStartTaskTwice() {
        givenPicker();
        givenLockedTask();

        task.setStatus("IN_PROGRESS");

        IllegalStateException exception = assertThrows(
                IllegalStateException.class,
                () -> service.start(5L, auth("picker"))
        );

        assertEquals(
                "Only ASSIGNED tasks can be started",
                exception.getMessage()
        );
    }

    @Test
    void pickerShouldCollectProducts() {
        givenPicker();
        givenLockedTask();

        task.setStatus("IN_PROGRESS");

        PickingTaskItem item = taskItem(11L, 3, 1);

        when(taskItems.findById(11L))
                .thenReturn(Optional.of(item));

        service.collect(
                5L,
                11L,
                new CollectRequest(1),
                auth("picker")
        );

        assertEquals(2, item.getCollectedQuantity());
        verify(taskItems).save(item);
    }

    @Test
    void shouldNotCollectBeforeStartingTask() {
        givenPicker();
        givenLockedTask();

        assertThrows(
                IllegalStateException.class,
                () -> service.collect(
                        5L,
                        11L,
                        new CollectRequest(1),
                        auth("picker")
                )
        );

        verify(taskItems, never()).save(any());
    }

    @Test
    void shouldRejectExcessCollectedQuantity() {
        givenPicker();
        givenLockedTask();

        task.setStatus("IN_PROGRESS");

        PickingTaskItem item = taskItem(11L, 3, 2);

        when(taskItems.findById(11L))
                .thenReturn(Optional.of(item));

        assertThrows(
                IllegalStateException.class,
                () -> service.collect(
                        5L,
                        11L,
                        new CollectRequest(2),
                        auth("picker")
                )
        );

        assertEquals(2, item.getCollectedQuantity());
        verify(taskItems, never()).save(any());
    }

    @Test
    void shouldRejectItemFromAnotherTask() {
        givenPicker();
        givenLockedTask();

        task.setStatus("IN_PROGRESS");

        PickingTaskItem item = taskItem(11L, 3, 0);
        PickingTask anotherTask =
                withId(new PickingTask(), 99L);

        item.setTask(anotherTask);

        when(taskItems.findById(11L))
                .thenReturn(Optional.of(item));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.collect(
                        5L,
                        11L,
                        new CollectRequest(1),
                        auth("picker")
                )
        );
    }

    @Test
    void pickerShouldCompleteCollectedTask() {
        givenPicker();
        givenLockedTask();

        task.setStatus("IN_PROGRESS");

        PickingTaskItem item = taskItem(11L, 2, 2);

        when(taskItems.findByTaskId(5L))
                .thenReturn(List.of(item));

        TaskResponse result =
                service.complete(5L, auth("picker"));

        assertEquals("COMPLETED", result.status());
        assertNotNull(task.getCompletedAt());
        assertEquals(2, result.items().get(0).collectedQuantity());
    }

    @Test
    void shouldRejectIncompleteTask() {
        givenPicker();
        givenLockedTask();

        task.setStatus("IN_PROGRESS");

        when(taskItems.findByTaskId(5L))
                .thenReturn(List.of(taskItem(11L, 3, 1)));

        assertThrows(
                IllegalStateException.class,
                () -> service.complete(5L, auth("picker"))
        );

        assertEquals("IN_PROGRESS", task.getStatus());
    }

    @Test
    void shouldNotCompleteTaskBeforeStart() {
        givenPicker();
        givenLockedTask();

        assertThrows(
                IllegalStateException.class,
                () -> service.complete(5L, auth("picker"))
        );

        assertEquals("ASSIGNED", task.getStatus());
    }

    @Test
    void managerShouldAssignTaskToPicker() {
        givenManager();
        givenLockedTask();

        User newPicker = user(8L, "new_picker", "PICKER");

        when(users.findById(8L))
                .thenReturn(Optional.of(newPicker));

        TaskResponse result = service.assign(
                5L,
                new AssignRequest(8L),
                auth("manager")
        );

        assertEquals(8L, result.assignedPickerId());
        assertEquals("ASSIGNED", result.status());
    }

    @Test
    void shouldNotReassignTaskInProgress() {
        givenManager();
        givenLockedTask();

        task.setStatus("IN_PROGRESS");

        assertThrows(
                IllegalStateException.class,
                () -> service.assign(
                        5L,
                        new AssignRequest(8L),
                        auth("manager")
                )
        );

        verify(users, never()).findById(anyLong());
    }

    @Test
    void shouldRejectAssigningManagerAsPicker() {
        givenManager();
        givenLockedTask();

        when(users.findById(1L))
                .thenReturn(Optional.of(manager));

        assertThrows(
                IllegalArgumentException.class,
                () -> service.assign(
                        5L,
                        new AssignRequest(1L),
                        auth("manager")
                )
        );
    }

    @Test
    void managerShouldCancelAssignedTask() {
        givenManager();
        givenLockedTask();

        TaskResponse result =
                service.cancel(5L, auth("manager"));

        assertEquals("CANCELLED", result.status());
    }

    @Test
    void shouldNotCancelCompletedTask() {
        givenManager();
        givenLockedTask();

        task.setStatus("COMPLETED");

        assertThrows(
                IllegalStateException.class,
                () -> service.cancel(5L, auth("manager"))
        );

        assertEquals("COMPLETED", task.getStatus());
    }

    @Test
    void assignedPickerShouldCalculateRoute() {
        givenPicker();

        when(tasks.findById(5L))
                .thenReturn(Optional.of(task));

        assertDoesNotThrow(
                () -> service.assertRouteAccessible(
                        5L,
                        auth("picker")
                )
        );
    }

    @Test
    void shouldRejectRouteForCancelledTask() {
        givenManager();

        task.setStatus("CANCELLED");

        when(tasks.findById(5L))
                .thenReturn(Optional.of(task));

        assertThrows(
                IllegalStateException.class,
                () -> service.assertRouteAccessible(
                        5L,
                        auth("manager")
                )
        );
    }

    @Test
    void shouldRejectRouteForCompletedPickerTask() {
        givenPicker();

        task.setStatus("COMPLETED");

        when(tasks.findById(5L))
                .thenReturn(Optional.of(task));

        assertThrows(
                IllegalStateException.class,
                () -> service.assertRouteAccessible(
                        5L,
                        auth("picker")
                )
        );
    }

    @Test
    void shouldRejectProductsForUnknownWarehouse() {
        when(warehouses.existsById(999L))
                .thenReturn(false);

        assertThrows(
                NoSuchElementException.class,
                () -> service.availableProducts(999L)
        );
    }

    @Test
    void shouldAggregateAvailableProductQuantities() {
        when(warehouses.existsById(1L))
                .thenReturn(true);

        Product product = product(10L);

        ProductLocation first = new ProductLocation();
        first.setProduct(product);
        first.setQuantity(2);

        ProductLocation second = new ProductLocation();
        second.setProduct(product);
        second.setQuantity(3);

        when(locations.findByNodeWarehouseId(1L))
                .thenReturn(List.of(first, second));

        List<ProductOption> result =
                service.availableProducts(1L);

        assertEquals(1, result.size());
        assertEquals(5, result.get(0).availableQuantity());
        assertEquals(10L, result.get(0).id());
    }

    private void givenManager() {
        when(users.findByUsername("manager"))
                .thenReturn(Optional.of(manager));
    }

    private void givenPicker() {
        when(users.findByUsername("picker"))
                .thenReturn(Optional.of(picker));
    }

    private void givenLockedTask() {
        when(tasks.findByIdForUpdate(5L))
                .thenReturn(Optional.of(task));
    }

    private Authentication auth(String username) {
        return new UsernamePasswordAuthenticationToken(
                username,
                "test"
        );
    }

    private User user(
            Long id,
            String username,
            String role
    ) {
        User result = withId(new User(), id);
        result.setUsername(username);
        result.setRole(role);
        return result;
    }

    private Product product(Long id) {
        Product result = withId(new Product(), id);
        result.setSku("TEST-001");
        result.setName("Test Product");
        result.setWeightKg(new BigDecimal("0.500"));
        return result;
    }

    private PickingTaskItem taskItem(
            Long id,
            int required,
            int collected
    ) {
        PickingTaskItem result =
                withId(new PickingTaskItem(), id);

        result.setTask(task);
        result.setProduct(product(10L));
        result.setRequiredQuantity(required);
        result.setCollectedQuantity(collected);

        return result;
    }

    private <T> T withId(T entity, Long id) {
        try {
            Field field = entity.getClass()
                    .getDeclaredField("id");

            field.setAccessible(true);
            field.set(entity, id);

            return entity;
        }
        catch (ReflectiveOperationException exception) {
            throw new IllegalStateException(
                    "Failed to initialize test entity",
                    exception
            );
        }
    }
}
