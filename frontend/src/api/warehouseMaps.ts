
import { apiRequest } from "./client";
import type { Credentials } from "./client";
import type { WarehouseMap } from "../types/warehouseMap";

export function getWarehouseMap(
    warehouseId: number,
    credentials: Credentials
): Promise<WarehouseMap> {
    return apiRequest<WarehouseMap>(
        `/warehouses/${warehouseId}/map`,
        credentials
    );
}

export function importWarehouseMap(
    warehouseId: number,
    map: WarehouseMap,
    credentials: Credentials
): Promise<WarehouseMap> {
    return apiRequest<WarehouseMap>(
        `/warehouses/${warehouseId}/map/import`,
        credentials,
        {
            method: "POST",
            body: JSON.stringify(map),
        }
    );
}
