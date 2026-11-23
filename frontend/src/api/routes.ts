
import { apiRequest } from "./client";
import type { Credentials } from "./client";
import type { RouteComparison } from "../types/route";

export function calculateRoute(
    taskId: number,
    credentials: Credentials
): Promise<RouteComparison> {
    return apiRequest<RouteComparison>(
        `/routes/calculate/${taskId}`,
        credentials,
        {
            method: "POST",
        }
    );
}
