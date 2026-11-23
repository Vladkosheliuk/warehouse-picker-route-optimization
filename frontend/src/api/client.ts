
export interface Credentials {
    username: string;
    password: string;
}

export async function apiRequest<T>(
    endpoint: string,
    credentials: Credentials,
    options: RequestInit = {}
): Promise<T> {
    const bytes = new TextEncoder().encode(
        `${credentials.username}:${credentials.password}`
    );

    const binary = Array.from(
        bytes,
        byte => String.fromCharCode(byte)
    ).join("");

    const authorization = `Basic ${btoa(binary)}`;


    const headers = new Headers(options.headers);

    headers.set("Authorization", authorization);
    headers.set("X-Requested-With", "XMLHttpRequest");


    if (options.body) {
        headers.set("Content-Type", "application/json");
    }

    const response = await fetch(`/api${endpoint}`, {
        ...options,
        headers,
    });

    if (!response.ok) {
        let message = `Request failed: HTTP ${response.status}`;

        if (response.status === 401) {
            message = "Invalid username or password";
        } else if (response.status === 403) {
            message = "You do not have permission";
        } else {
            try {
                const error = await response.json();
                if (typeof error.message === "string") {
                    message = error.message;
                }
            } catch {
            }
        }

        throw new Error(message);
    }

    if (response.status === 204) {
        return undefined as T;
    }

    return (await response.json()) as T;
}
