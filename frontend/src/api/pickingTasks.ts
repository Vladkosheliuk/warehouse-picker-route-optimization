import { apiRequest } from "./client";
import type { Credentials } from "./client";
import type { RouteComparison } from "../types/route";

export interface PickingTaskItem {
    id: number;
    productId: number;
    sku: string;
    productName: string;
    requiredQuantity: number;
    collectedQuantity: number;
}

export type PickingTaskStatus =
    | "NEW"
    | "ASSIGNED"
    | "IN_PROGRESS"
    | "COMPLETED"
    | "CANCELLED";

export interface PickingTask {
    id: number;
    warehouseId: number;
    warehouseName: string;
    assignedPickerId: number | null;
    assignedPickerUsername: string | null;
    status: PickingTaskStatus;
    createdAt: string | null;
    startedAt: string | null;
    completedAt: string | null;
    items: PickingTaskItem[];
}

export interface ProductOption {
    id: number;
    sku: string;
    name: string;
    weightKg: number;
    availableQuantity: number;
}

export interface CreatePickingTask {
    warehouseId: number;
    pickerId: number;
    items: {
        productId: number;
        quantity: number;
    }[];
}

export function listTasks(credentials: Credentials) {
    return apiRequest<PickingTask[]>("/tasks", credentials);
}

export function listMyTasks(credentials: Credentials) {
    return apiRequest<PickingTask[]>("/tasks/my", credentials);
}

export function listAvailableProducts(
    warehouseId: number,
    credentials: Credentials
) {
    return apiRequest<ProductOption[]>(
        `/catalog/products?warehouseId=${warehouseId}`,
        credentials
    );
}

export function createPickingTask(
    data: CreatePickingTask,
    credentials: Credentials
) {
    return apiRequest<PickingTask>("/tasks", credentials, {
        method: "POST",
        body: JSON.stringify(data),
    });
}

export function assignPickingTask(
    id: number,
    pickerId: number,
    credentials: Credentials
) {
    return apiRequest<PickingTask>(
        `/tasks/${id}/assign`,
        credentials,
        {
            method: "POST",
            body: JSON.stringify({ pickerId }),
        }
    );
}

export function startPickingTask(
    id: number,
    credentials: Credentials
) {
    return apiRequest<PickingTask>(
        `/tasks/${id}/start`,
        credentials,
        { method: "POST" }
    );
}

export function collectPickingItem(
    taskId: number,
    itemId: number,
    quantity: number,
    credentials: Credentials
) {
    return apiRequest<PickingTask>(
        `/tasks/${taskId}/items/${itemId}/collect`,
        credentials,
        {
            method: "POST",
            body: JSON.stringify({ quantity }),
        }
    );
}

export function completePickingTask(
    id: number,
    credentials: Credentials
) {
    return apiRequest<PickingTask>(
        `/tasks/${id}/complete`,
        credentials,
        { method: "POST" }
    );
}

export function cancelPickingTask(
    id: number,
    credentials: Credentials
) {
    return apiRequest<PickingTask>(
        `/tasks/${id}/cancel`,
        credentials,
        { method: "POST" }
    );
}

export function calculateTaskRoute(
    id: number,
    credentials: Credentials
) {
    return apiRequest<RouteComparison>(
        `/routes/calculate/${id}`,
        credentials,
        { method: "POST" }
    );
}
