package com.warehouse.picker.repository;

import com.warehouse.picker.entity.PickingRoute;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PickingRouteRepository
        extends JpaRepository<PickingRoute, Long> {

    List<PickingRoute> findByTaskId(Long taskId);

    List<PickingRoute> findByTaskIdAndAlgorithm(
            Long taskId,
            String algorithm
    );
}
