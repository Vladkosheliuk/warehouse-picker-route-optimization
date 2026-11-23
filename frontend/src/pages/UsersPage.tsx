
import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import type { Credentials } from "../api/client";
import {
    createUser,
    getUsers,
} from "../api/users";
import type {
    UserRole,
    WarehouseUser,
} from "../api/users";
import "./UsersPage.css";

interface Props {
    credentials: Credentials;
}

export default function UsersPage({ credentials }: Props) {
    const [users, setUsers] = useState<WarehouseUser[]>([]);

    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    const [username, setUsername] = useState("");
    const [fullName, setFullName] = useState("");
    const [password, setPassword] = useState("");
    const [role, setRole] = useState<UserRole>("PICKER");

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    useEffect(() => {
        let active = true;

        getUsers(credentials)
            .then((data) => {
                if (active) {
                    setUsers(data);
                }
            })
            .catch((err: unknown) => {
                if (active) {
                    setError(
                        err instanceof Error
                            ? err.message
                            : "Unable to load users."
                    );
                }
            })
            .finally(() => {
                if (active) setLoading(false);
            });

        return () => {
            active = false;
        };
    }, [credentials]);

    async function refreshUsers() {
        setLoading(true);
        setError("");

        try {
            const data = await getUsers(credentials);
            setUsers(data);
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Unable to refresh users."
            );
        } finally {
            setLoading(false);
        }
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (saving || loading) return;

        const normalizedUsername = username.trim();
        const normalizedFullName = fullName.trim();

        if (!/^[A-Za-z0-9._-]{3,100}$/.test(normalizedUsername)) {
            setError(
                "Username must be 3–100 characters and contain only " +
                "letters, numbers, dots, underscores or hyphens."
            );
            return;
        }

        if (
            normalizedFullName.length === 0 ||
            normalizedFullName.length > 150
        ) {
            setError("Full name must contain 1–150 characters.");
            return;
        }

        if (password.length < 12 || password.length > 72) {
            setError("Password must contain 12–72 characters.");
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        try {
            const created = await createUser(
                {
                    username: normalizedUsername,
                    fullName: normalizedFullName,
                    password,
                    role,
                },
                credentials
            );

            setUsers((previous) => [...previous, created]);

            setUsername("");
            setFullName("");
            setPassword("");
            setRole("PICKER");

            setSuccess(
                `Account "${created.username}" created successfully.`
            );
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Unable to create user."
            );
        } finally {
            setSaving(false);
        }
    }

    const managers = users.filter(
        (user) => user.role === "MANAGER"
    ).length;

    const pickers = users.filter(
        (user) => user.role === "PICKER"
    ).length;

    return (
        <div className="users-page">
            <div className="page-heading">
                <span className="eyebrow">TEAM MANAGEMENT</span>
                <h1>User Management</h1>
                <p>
                    Manage warehouse staff accounts and assign
                    Manager or Picker access.
                </p>
            </div>

            <div className="users-stats">
                <div className="users-stat">
                    <span>Total users</span>
                    <strong>{users.length}</strong>
                </div>

                <div className="users-stat">
                    <span>Managers</span>
                    <strong>{managers}</strong>
                </div>

                <div className="users-stat">
                    <span>Pickers</span>
                    <strong>{pickers}</strong>
                </div>
            </div>

            {error && (
                <div className="warehouse-alert error" role="alert">
                    {error}
                </div>
            )}

            {success && (
                <div className="warehouse-alert success" role="status">
                    {success}
                </div>
            )}

            <div className="users-layout">
                <section className="users-panel">
                    <div className="users-panel-header">
                        <div>
                            <span className="eyebrow">EMPLOYEE DIRECTORY</span>
                            <h2>Staff accounts</h2>
                        </div>

                        <button
                            type="button"
                            className="warehouse-secondary-btn"
                            onClick={refreshUsers}
                            disabled={loading || saving}
                        >
                            Refresh
                        </button>
                    </div>

                    {loading ? (
                        <div className="users-empty" role="status">
                            Loading users...
                        </div>
                    ) : users.length === 0 ? (
                        <div className="users-empty">
                            No users found.
                        </div>
                    ) : (
                        <div className="warehouse-table-wrapper">
                            <table className="warehouse-table">
                                <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Employee</th>
                                    <th>Username</th>
                                    <th>Role</th>
                                    <th>Status</th>
                                </tr>
                                </thead>

                                <tbody>
                                {users.map((user) => (
                                    <tr key={user.id}>
                                        <td>
                        <span className="warehouse-id">
                          #{user.id}
                        </span>
                                        </td>

                                        <td>
                                            <strong>{user.fullName}</strong>
                                        </td>

                                        <td>{user.username}</td>

                                        <td>
                        <span
                            className={
                                user.role === "MANAGER"
                                    ? "user-role manager"
                                    : "user-role picker"
                            }
                        >
                          {user.role}
                        </span>
                                        </td>

                                        <td>
                        <span
                            className={
                                user.active
                                    ? "user-status active"
                                    : "user-status inactive"
                            }
                        >
                          {user.active ? "Active" : "Inactive"}
                        </span>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>

                <section className="users-panel">
                    <div className="users-panel-header">
                        <div>
                            <span className="eyebrow">NEW EMPLOYEE</span>
                            <h2>Create account</h2>
                        </div>
                    </div>

                    <form className="warehouse-form" onSubmit={handleSubmit}>
                        <label htmlFor="employee-full-name">
                            Full name
                        </label>

                        <input
                            id="employee-full-name"
                            value={fullName}
                            onChange={(event) =>
                                setFullName(event.target.value)
                            }
                            placeholder="e.g. John Smith"
                            maxLength={150}
                            required
                        />

                        <label htmlFor="employee-username">
                            Username
                        </label>

                        <input
                            id="employee-username"
                            value={username}
                            onChange={(event) =>
                                setUsername(event.target.value)
                            }
                            placeholder="e.g. john.smith"
                            minLength={3}
                            maxLength={100}
                            autoComplete="off"
                            required
                        />

                        <label htmlFor="employee-password">
                            Temporary password
                        </label>

                        <input
                            id="employee-password"
                            type="password"
                            value={password}
                            onChange={(event) =>
                                setPassword(event.target.value)
                            }
                            placeholder="At least 12 characters"
                            minLength={12}
                            maxLength={72}
                            autoComplete="new-password"
                            required
                        />

                        <label htmlFor="employee-role">
                            Account role
                        </label>

                        <select
                            id="employee-role"
                            className="users-select"
                            value={role}
                            onChange={(event) =>
                                setRole(event.target.value as UserRole)
                            }
                        >
                            <option value="PICKER">Picker</option>
                            <option value="MANAGER">Manager</option>
                        </select>

                        <div className="role-info">
                            {role === "MANAGER"
                                ? "Managers can manage warehouses and staff accounts."
                                : "Pickers can access permitted picking routes."}
                        </div>

                        <button
                            type="submit"
                            className="primary-button"
                            disabled={saving || loading}
                        >
                            {saving ? "Creating..." : "Create employee account"}
                        </button>
                    </form>

                    <p className="warehouse-help">
                        Accounts are created in PostgreSQL.
                        Passwords are stored by the backend as BCrypt hashes.
                        Only managers can use this feature.
                    </p>
                </section>
            </div>
        </div>
    );
}
