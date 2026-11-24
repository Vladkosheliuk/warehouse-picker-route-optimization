package com.warehouse.picker.service;

import com.warehouse.picker.entity.Warehouse;
import com.warehouse.picker.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WarehouseServiceTest {

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private WarehouseService warehouseService;

    private Warehouse warehouse;

    @BeforeEach
    void setUp() {
        warehouse = new Warehouse();
        warehouse.setName("Main Warehouse");
        warehouse.setDescription("Primary warehouse");
    }

    @Test
    void getAllWarehouses_shouldReturnWarehouseList() {
        when(warehouseRepository.findAll())
                .thenReturn(List.of(warehouse));

        List<Warehouse> result = warehouseService.getAllWarehouses();

        assertEquals(1, result.size());
        assertEquals("Main Warehouse", result.get(0).getName());

        verify(warehouseRepository).findAll();
    }

    @Test
    void getWarehouseById_shouldReturnWarehouseWhenExists() {
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        Warehouse result = warehouseService.getWarehouseById(1L);

        assertSame(warehouse, result);
        verify(warehouseRepository).findById(1L);
    }

    @Test
    void getWarehouseById_shouldThrowExceptionWhenNotFound() {
        when(warehouseRepository.findById(999L))
                .thenReturn(Optional.empty());

        NoSuchElementException exception = assertThrows(
                NoSuchElementException.class,
                () -> warehouseService.getWarehouseById(999L)
        );

        assertEquals(
                "Warehouse not found with id: 999",
                exception.getMessage()
        );

        verify(warehouseRepository).findById(999L);
    }

    @Test
    void getAllWarehouses_shouldReturnEmptyList() {
        when(warehouseRepository.findAll())
                .thenReturn(List.of());

        List<Warehouse> result = warehouseService.getAllWarehouses();

        assertTrue(result.isEmpty());
        verify(warehouseRepository).findAll();
    }

    @Test
    void createWarehouse_shouldSaveWarehouse() {
        when(warehouseRepository.save(warehouse))
                .thenReturn(warehouse);

        Warehouse result = warehouseService.createWarehouse(warehouse);

        assertSame(warehouse, result);
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void updateWarehouse_shouldUpdateExistingWarehouse() {
        Warehouse updatedData = new Warehouse();
        updatedData.setName("Updated Warehouse");
        updatedData.setDescription("Updated description");

        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        when(warehouseRepository.save(warehouse))
                .thenReturn(warehouse);

        Warehouse result = warehouseService.updateWarehouse(1L, updatedData);

        assertEquals("Updated Warehouse", result.getName());
        assertEquals("Updated description", result.getDescription());

        verify(warehouseRepository).findById(1L);
        verify(warehouseRepository).save(warehouse);
    }

    @Test
    void updateWarehouse_shouldThrowExceptionWhenNotFound() {
        Warehouse updatedData = new Warehouse();
        updatedData.setName("Updated Warehouse");

        when(warehouseRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> warehouseService.updateWarehouse(999L, updatedData)
        );

        verify(warehouseRepository, never()).save(any());
    }

    @Test
    void deleteWarehouse_shouldDeleteExistingWarehouse() {
        when(warehouseRepository.findById(1L))
                .thenReturn(Optional.of(warehouse));

        warehouseService.deleteWarehouse(1L);

        verify(warehouseRepository).findById(1L);
        verify(warehouseRepository).delete(warehouse);
    }

    @Test
    void deleteWarehouse_shouldThrowExceptionWhenNotFound() {
        when(warehouseRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> warehouseService.deleteWarehouse(999L)
        );

        verify(warehouseRepository, never()).delete(any());
    }
}
