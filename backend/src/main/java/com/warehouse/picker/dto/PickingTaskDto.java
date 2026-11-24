package com.warehouse.picker.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public final class PickingTaskDto {

    private PickingTaskDto() {}

    public record CreateRequest(
            @NotNull @Positive Long warehouseId,
            @NotNull @Positive Long pickerId,
            @NotEmpty List<@NotNull @Valid ItemRequest> items
    ) {}

    public record ItemRequest(
            @NotNull @Positive Long productId,
            @NotNull @Positive Integer quantity
    ) {}

    public record AssignRequest(
            @NotNull @Positive Long pickerId
    ) {}

    public record CollectRequest(
            @NotNull @Positive Integer quantity
    ) {}

    public record ItemResponse(
            Long id,
            Long productId,
            String sku,
            String productName,
            int requiredQuantity,
            int collectedQuantity
    ) {}

    public record TaskResponse(
            Long id,
            Long warehouseId,
            String warehouseName,
            Long assignedPickerId,
            String assignedPickerUsername,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime startedAt,
            OffsetDateTime completedAt,
            List<ItemResponse> items
    ) {}

    public record ProductOption(
            Long id,
            String sku,
            String name,
            BigDecimal weightKg,
            int availableQuantity
    ) {}
}
