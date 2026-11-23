package com.warehouse.picker.service;

import com.warehouse.picker.entity.Warehouse;
import com.warehouse.picker.repository.WarehouseRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class WarehouseService {

    private final WarehouseRepository warehouseRepository;

    public WarehouseService(WarehouseRepository warehouseRepository){
        this.warehouseRepository = warehouseRepository;
    }

    @Transactional(readOnly = true)
    public List<Warehouse> getAllWarehouses(){
        return warehouseRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Warehouse getWarehouseById(Long id) {
        return warehouseRepository.findById(id)
                .orElseThrow(() ->
                        new NoSuchElementException(
                                "Warehouse not found with id: " + id
                        )
                );
    }

    @Transactional
    public Warehouse createWarehouse(Warehouse warehouse){
        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public Warehouse updateWarehouse(Long id, Warehouse updateWarehouse){
        Warehouse warehouse = getWarehouseById(id);

        warehouse.setName(updateWarehouse.getName());
        warehouse.setDescription(updateWarehouse.getDescription());

        return warehouseRepository.save(warehouse);
    }

    @Transactional
    public void deleteWarehouse(Long id) {
        Warehouse warehouse = getWarehouseById(id);
        warehouseRepository.delete(warehouse);
    }


}
