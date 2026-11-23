package com.warehouse.picker.repository;

import com.warehouse.picker.entity.PickingTask;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickingTaskRepository
        extends JpaRepository<PickingTask, Long> {

    List<PickingTask> findByWarehouseId(Long warehouseId);

    List<PickingTask> findByAssignedPickerId(Long pickerId);

    List<PickingTask> findByStatus(String status);

    List<PickingTask> findByWarehouseIdAndStatus(
            Long warehouseId,
            String status
    );
}
