
package com.warehouse.picker.controller;

import com.warehouse.picker.dto.PickingTaskDto.ProductOption;
import com.warehouse.picker.service.PickingTaskService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/catalog")
@Tag(name = "Product Catalog")
public class ProductCatalogController {

    private final PickingTaskService service;

    public ProductCatalogController(PickingTaskService service) {
        this.service = service;
    }

    @GetMapping("/products")
    @Operation(
            summary = "List available products in a warehouse (MANAGER)"
    )
    public List<ProductOption> getProducts(
            @RequestParam Long warehouseId
    ) {
        return service.availableProducts(warehouseId);
    }
}
