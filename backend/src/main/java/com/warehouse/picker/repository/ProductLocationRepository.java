package com.warehouse.picker.repository;

import com.warehouse.picker.entity.ProductLocation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductLocationRepository
        extends JpaRepository<ProductLocation, Long> {

    List<ProductLocation> findByProductId(Long productid);

    List<ProductLocation> findByNodeWarehouseId(Long warehouseId);

    Optional<ProductLocation> findByProductIdAndNodeId(
            Long productId,
            Long nodeId
    );
}
