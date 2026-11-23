
import { apiRequest } from "./client";
import type { Credentials } from "./client";
import type {
    Warehouse,
    WarehouseInput,
} from "../types/warehouse";

export function getWarehouses(
    credentials: Credentials
): Promise<Warehouse[]> {
    return apiRequest<Warehouse[]>(
        "/warehouses",
        credentials
    );
}

export function getWarehouseById(
    id: number,
    credentials: Credentials
): Promise<Warehouse> {
    return apiRequest<Warehouse>(
        `/warehouses/${id}`,
        credentials
    );
}

export function createWarehouse(
    data: WarehouseInput,
    credentials: Credentials
): Promise<Warehouse> {
    return apiRequest<Warehouse>(
        "/warehouses",
        credentials,
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}

export function updateWarehouse(
    id: number,
    data: WarehouseInput,
    credentials: Credentials
): Promise<Warehouse> {
    return apiRequest<Warehouse>(
        `/warehouses/${id}`,
        credentials,
        {
            method: "PUT",
            body: JSON.stringify(data),
        }
    );
}

export function deleteWarehouse(
    id: number,
    credentials: Credentials
): Promise<void> {
    return apiRequest<void>(
        `/warehouses/${id}`,
        credentials,
        {
            method: "DELETE",
        }
    );
}
