package com.warehouse.picker.mapper;

import com.warehouse.picker.dto.WarehouseDTO;
import com.warehouse.picker.entity.Warehouse;
import org.springframework.stereotype.Component;

@Component
public class WarehouseMapper {

    public WarehouseDTO toDTO(Warehouse warehouse){
        if (warehouse == null) {
            return null;
        }

        return new WarehouseDTO(
                warehouse.getId(),
                warehouse.getName(),
                warehouse.getDescription()
        );
    }
    public Warehouse toEntity(WarehouseDTO dto){
        if (dto == null){
            return null;
        }

        Warehouse warehouse = new Warehouse();
        warehouse.setName(dto.getName());
        warehouse.setDescription(dto.getDescription());

        return warehouse;
    }
}
