
package com.warehouse.picker.service;

import com.warehouse.picker.dto.PickingTaskDto.*;
import com.warehouse.picker.entity.*;
import com.warehouse.picker.repository.*;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class PickingTaskService {

    private final PickingTaskRepository tasks;
    private final PickingTaskItemRepository taskItems;
    private final WarehouseRepository warehouses;
    private final UserRepository users;
    private final ProductRepository products;
    private final ProductLocationRepository locations;

    public PickingTaskService(
            PickingTaskRepository tasks,
            PickingTaskItemRepository taskItems,
            WarehouseRepository warehouses,
            UserRepository users,
            ProductRepository products,
            ProductLocationRepository locations
    ) {
        this.tasks = tasks;
        this.taskItems = taskItems;
        this.warehouses = warehouses;
        this.users = users;
        this.products = products;
        this.locations = locations;
    }

    private User currentUser(Authentication authentication) {
        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED
            );
        }

        User user = users.findByUsername(authentication.getName())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED
                        )
                );

        if (!user.isActive()) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Account is inactive"
            );
        }

        return user;
    }

    private void requireManager(User user) {
        if (!"MANAGER".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Manager access required"
            );
        }
    }

    private void requirePicker(User user) {
        if (!"PICKER".equals(user.getRole())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Picker access required"
            );
        }
    }

    private void checkOwnership(PickingTask task, User user) {
        if ("MANAGER".equals(user.getRole())) {
            return;
        }

        if (!"PICKER".equals(user.getRole())
                || task.getAssignedPicker() == null
                || !task.getAssignedPicker().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Task is not assigned to this account"
            );
        }
    }

    private PickingTask findTask(Long id) {
        return tasks.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Picking task not found: " + id
                        )
                );
    }

    private PickingTask lockTask(Long id) {
        return tasks.findByIdForUpdate(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Picking task not found: " + id
                        )
                );
    }

    private User findPicker(Long id) {
        User picker = users.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Picker not found: " + id
                        )
                );

        if (!picker.isActive()
                || !"PICKER".equals(picker.getRole())) {
            throw new IllegalArgumentException(
                    "Selected user is not an active PICKER"
            );
        }

        return picker;
    }

    private TaskResponse toResponse(PickingTask task) {
        List<ItemResponse> lines = taskItems
                .findByTaskId(task.getId())
                .stream()
                .sorted(Comparator.comparing(
                        PickingTaskItem::getId
                ))
                .map(item -> new ItemResponse(
                        item.getId(),
                        item.getProduct().getId(),
                        item.getProduct().getSku(),
                        item.getProduct().getName(),
                        item.getRequiredQuantity(),
                        item.getCollectedQuantity()
                ))
                .toList();

        User picker = task.getAssignedPicker();

        return new TaskResponse(
                task.getId(),
                task.getWarehouse().getId(),
                task.getWarehouse().getName(),
                picker == null ? null : picker.getId(),
                picker == null ? null : picker.getUsername(),
                task.getStatus(),
                task.getCreatedAt(),
                task.getStartedAt(),
                task.getCompletedAt(),
                lines
        );
    }

    @Transactional(readOnly = true)
    public List<ProductOption> availableProducts(Long warehouseId) {
        if (!warehouses.existsById(warehouseId)) {
            throw new NoSuchElementException(
                    "Warehouse not found: " + warehouseId
            );
        }

        Map<Long, ProductOption> result = new LinkedHashMap<>();

        for (ProductLocation location :
                locations.findByNodeWarehouseId(warehouseId)) {

            Product product = location.getProduct();
            ProductOption previous = result.get(product.getId());

            int quantity =
                    (previous == null ? 0 : previous.availableQuantity())
                            + location.getQuantity();

            result.put(
                    product.getId(),
                    new ProductOption(
                            product.getId(),
                            product.getSku(),
                            product.getName(),
                            product.getWeightKg(),
                            quantity
                    )
            );
        }

        return result.values().stream()
                .filter(product -> product.availableQuantity() > 0)
                .sorted(Comparator.comparing(ProductOption::sku))
                .toList();
    }

    @Transactional
    public TaskResponse create(
            CreateRequest request,
            Authentication authentication
    ) {
        User manager = currentUser(authentication);
        requireManager(manager);

        Warehouse warehouse = warehouses.findById(request.warehouseId())
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Warehouse not found: " + request.warehouseId()
                        )
                );

        User picker = findPicker(request.pickerId());

        Set<Long> uniqueProducts = new HashSet<>();

        Map<Long, Integer> available = availableProducts(
                warehouse.getId()
        ).stream().collect(Collectors.toMap(
                ProductOption::id,
                ProductOption::availableQuantity
        ));

        for (ItemRequest line : request.items()) {
            if (!uniqueProducts.add(line.productId())) {
                throw new IllegalArgumentException(
                        "Duplicate product in task: " + line.productId()
                );
            }

            if (!products.existsById(line.productId())) {
                throw new NoSuchElementException(
                        "Product not found: " + line.productId()
                );
            }

            if (available.getOrDefault(line.productId(), 0)
                    < line.quantity()) {
                throw new IllegalStateException(
                        "Insufficient stock in selected warehouse " +
                                "for product: " + line.productId()
                );
            }
        }

        PickingTask task = new PickingTask();
        task.setWarehouse(warehouse);
        task.setCreatedBy(manager);
        task.setAssignedPicker(picker);
        task.setStatus("ASSIGNED");

        task = tasks.saveAndFlush(task);

        List<PickingTaskItem> items = new ArrayList<>();

        for (ItemRequest line : request.items()) {
            PickingTaskItem item = new PickingTaskItem();

            item.setTask(task);
            item.setProduct(products.getReferenceById(
                    line.productId()
            ));
            item.setRequiredQuantity(line.quantity());
            item.setCollectedQuantity(0);

            items.add(item);
        }

        taskItems.saveAllAndFlush(items);

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listAll(Authentication authentication) {
        requireManager(currentUser(authentication));

        return tasks.findAll().stream()
                .sorted(Comparator.comparing(
                        PickingTask::getId
                ).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TaskResponse> listMine(Authentication authentication) {
        User picker = currentUser(authentication);
        requirePicker(picker);

        return tasks.findByAssignedPickerId(picker.getId())
                .stream()
                .sorted(Comparator.comparing(
                        PickingTask::getId
                ).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public TaskResponse findOne(
            Long id,
            Authentication authentication
    ) {
        User actor = currentUser(authentication);
        PickingTask task = findTask(id);

        checkOwnership(task, actor);

        return toResponse(task);
    }

    @Transactional
    public TaskResponse assign(
            Long id,
            AssignRequest request,
            Authentication authentication
    ) {
        requireManager(currentUser(authentication));

        PickingTask task = lockTask(id);

        if (!"NEW".equals(task.getStatus())
                && !"ASSIGNED".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "Only NEW or ASSIGNED tasks can be assigned"
            );
        }

        task.setAssignedPicker(findPicker(request.pickerId()));
        task.setStatus("ASSIGNED");

        return toResponse(task);
    }

    @Transactional
    public TaskResponse start(
            Long id,
            Authentication authentication
    ) {
        User picker = currentUser(authentication);
        requirePicker(picker);

        PickingTask task = lockTask(id);
        checkOwnership(task, picker);

        if (!"ASSIGNED".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "Only ASSIGNED tasks can be started"
            );
        }

        task.setStatus("IN_PROGRESS");
        task.setStartedAt(OffsetDateTime.now());

        return toResponse(task);
    }

    @Transactional
    public TaskResponse collect(
            Long taskId,
            Long itemId,
            CollectRequest request,
            Authentication authentication
    ) {
        User picker = currentUser(authentication);
        requirePicker(picker);

        PickingTask task = lockTask(taskId);
        checkOwnership(task, picker);

        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "Start the picking task before collecting products"
            );
        }

        PickingTaskItem item = taskItems.findById(itemId)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Task item not found: " + itemId
                        )
                );

        if (!item.getTask().getId().equals(taskId)) {
            throw new IllegalArgumentException(
                    "Item does not belong to this task"
            );
        }

        long newQuantity =
                (long) item.getCollectedQuantity() + request.quantity();

        if (newQuantity > item.getRequiredQuantity()) {
            throw new IllegalStateException(
                    "Collected quantity cannot exceed required quantity"
            );
        }

        item.setCollectedQuantity((int) newQuantity);
        taskItems.save(item);

        return toResponse(task);
    }

    @Transactional
    public TaskResponse complete(
            Long id,
            Authentication authentication
    ) {
        User picker = currentUser(authentication);
        requirePicker(picker);

        PickingTask task = lockTask(id);
        checkOwnership(task, picker);

        if (!"IN_PROGRESS".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "Task is not IN_PROGRESS"
            );
        }

        List<PickingTaskItem> items =
                taskItems.findByTaskId(id);

        if (items.isEmpty() || items.stream().anyMatch(
                item -> !item.getRequiredQuantity()
                        .equals(item.getCollectedQuantity())
        )) {
            throw new IllegalStateException(
                    "All task items must be collected before completion"
            );
        }

        task.setStatus("COMPLETED");
        task.setCompletedAt(OffsetDateTime.now());

        return toResponse(task);
    }

    @Transactional
    public TaskResponse cancel(
            Long id,
            Authentication authentication
    ) {
        requireManager(currentUser(authentication));

        PickingTask task = lockTask(id);

        if ("COMPLETED".equals(task.getStatus())
                || "CANCELLED".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "This task cannot be cancelled"
            );
        }

        task.setStatus("CANCELLED");

        return toResponse(task);
    }

    @Transactional(readOnly = true)
    public void assertRouteAccessible(
            Long taskId,
            Authentication authentication
    ) {
        User actor = currentUser(authentication);
        PickingTask task = findTask(taskId);

        checkOwnership(task, actor);

        if ("CANCELLED".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "Cannot calculate a cancelled task"
            );
        }

        if ("PICKER".equals(actor.getRole())
                && !"ASSIGNED".equals(task.getStatus())
                && !"IN_PROGRESS".equals(task.getStatus())) {
            throw new IllegalStateException(
                    "Task is not available for picking"
            );
        }
    }
}
