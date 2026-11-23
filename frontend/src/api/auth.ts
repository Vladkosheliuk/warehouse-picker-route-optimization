
import { apiRequest } from "./client";
import type { Credentials } from "./client";

export type UserRole = "MANAGER" | "PICKER";

export interface CurrentUser {
    username: string;
    role: UserRole;
}

export async function getCurrentUser(
    credentials: Credentials
): Promise<CurrentUser> {
    const user = await apiRequest<CurrentUser>(
        "/auth/me",
        credentials
    );

    if (user.role !== "MANAGER" && user.role !== "PICKER") {
        throw new Error("Unsupported user role");
    }

    return user;
}
