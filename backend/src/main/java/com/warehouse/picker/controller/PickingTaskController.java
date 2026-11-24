package com.warehouse.picker.controller;

import com.warehouse.picker.dto.PickingTaskDto.*;
import com.warehouse.picker.service.PickingTaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@Tag(
        name = "Picking Tasks",
        description = "Create, assign and process picking tasks"
)
public class PickingTaskController {

    private final PickingTaskService service;

    public PickingTaskController(PickingTaskService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "List all tasks (MANAGER)")
    public List<TaskResponse> getAll(Authentication authentication) {
        return service.listAll(authentication);
    }

    @GetMapping("/my")
    @Operation(summary = "List my assigned tasks (PICKER)")
    public List<TaskResponse> getMine(Authentication authentication) {
        return service.listMine(authentication);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task details")
    public TaskResponse getById(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.findOne(id, authentication);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create and assign a task (MANAGER)")
    public TaskResponse create(
            @Valid @RequestBody CreateRequest request,
            Authentication authentication
    ) {
        return service.create(request, authentication);
    }

    @PostMapping("/{id}/assign")
    @Operation(summary = "Assign or reassign a task (MANAGER)")
    public TaskResponse assign(
            @PathVariable Long id,
            @Valid @RequestBody AssignRequest request,
            Authentication authentication
    ) {
        return service.assign(id, request, authentication);
    }

    @PostMapping("/{id}/start")
    @Operation(summary = "Start my assigned task (PICKER)")
    public TaskResponse start(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.start(id, authentication);
    }

    @PostMapping("/{id}/items/{itemId}/collect")
    @Operation(summary = "Mark collected product quantity (PICKER)")
    public TaskResponse collect(
            @PathVariable Long id,
            @PathVariable Long itemId,
            @Valid @RequestBody CollectRequest request,
            Authentication authentication
    ) {
        return service.collect(
                id,
                itemId,
                request,
                authentication
        );
    }

    @PostMapping("/{id}/complete")
    @Operation(summary = "Complete picking task (PICKER)")
    public TaskResponse complete(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.complete(id, authentication);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel picking task (MANAGER)")
    public TaskResponse cancel(
            @PathVariable Long id,
            Authentication authentication
    ) {
        return service.cancel(id, authentication);
    }
}
