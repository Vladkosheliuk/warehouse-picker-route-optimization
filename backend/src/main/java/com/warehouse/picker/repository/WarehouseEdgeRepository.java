package com.warehouse.picker.repository;

import com.warehouse.picker.entity.WarehouseEdge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseEdgeRepository
        extends JpaRepository<WarehouseEdge, Long> {

    List<WarehouseEdge> findByWarehouseId(Long warehouseId);
}
