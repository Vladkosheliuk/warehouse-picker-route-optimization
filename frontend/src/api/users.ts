
import { apiRequest } from "./client";
import type { Credentials } from "./client";

export type UserRole = "MANAGER" | "PICKER";

export interface WarehouseUser {
    id: number;
    username: string;
    fullName: string;
    role: UserRole;
    active: boolean;
}

export interface CreateUserRequest {
    username: string;
    fullName: string;
    password: string;
    role: UserRole;
}

export function getUsers(
    credentials: Credentials
): Promise<WarehouseUser[]> {
    return apiRequest<WarehouseUser[]>(
        "/users",
        credentials
    );
}

export function createUser(
    data: CreateUserRequest,
    credentials: Credentials
): Promise<WarehouseUser> {
    return apiRequest<WarehouseUser>(
        "/users",
        credentials,
        {
            method: "POST",
            body: JSON.stringify(data),
        }
    );
}
