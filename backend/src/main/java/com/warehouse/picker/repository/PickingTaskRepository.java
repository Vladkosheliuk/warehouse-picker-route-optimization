
package com.warehouse.picker.repository;

import com.warehouse.picker.entity.PickingTask;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PickingTaskRepository
        extends JpaRepository<PickingTask, Long> {

    List<PickingTask> findByWarehouseId(Long warehouseId);

    List<PickingTask> findByAssignedPickerId(Long pickerId);

    List<PickingTask> findByStatus(String status);

    List<PickingTask> findByWarehouseIdAndStatus(
            Long warehouseId,
            String status
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from PickingTask t where t.id = :id")
    Optional<PickingTask> findByIdForUpdate(
            @Param("id") Long id
    );
}
