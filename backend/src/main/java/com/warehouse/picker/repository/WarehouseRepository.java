
package com.warehouse.picker.repository;

import com.warehouse.picker.entity.Warehouse;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WarehouseRepository
        extends JpaRepository<Warehouse, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM Warehouse w WHERE w.id = :id")
    Optional<Warehouse> findByIdForUpdate(@Param("id") Long id);
}
