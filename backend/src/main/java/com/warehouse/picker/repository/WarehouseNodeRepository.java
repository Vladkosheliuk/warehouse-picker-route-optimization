package com.warehouse.picker.repository;

import com.warehouse.picker.entity.WarehouseNode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WarehouseNodeRepository
        extends JpaRepository<WarehouseNode, Long> {

    List<WarehouseNode> findByWarehouseId(Long warehouseId);

    Optional<WarehouseNode> findByWarehouseIdAndNodeCode(
            Long warehouseId,
            String nodeCode
    );
}
