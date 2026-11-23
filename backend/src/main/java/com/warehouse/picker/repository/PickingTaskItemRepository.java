package com.warehouse.picker.repository;

import com.warehouse.picker.entity.PickingTaskItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PickingTaskItemRepository
        extends JpaRepository<PickingTaskItem, Long> {

    List<PickingTaskItem> findByTaskId(Long taskId);

    Optional<PickingTaskItem> findByTaskIdAndProductId(
            Long taskId,
            Long productId
    );
}
