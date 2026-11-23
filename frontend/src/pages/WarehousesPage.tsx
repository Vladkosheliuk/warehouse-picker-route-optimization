
import { useEffect, useState } from "react";
import type { FormEvent } from "react";

import type { Credentials } from "../api/client";
import {
    getWarehouses,
    createWarehouse,
    updateWarehouse,
    deleteWarehouse,
} from "../api/warehouses";

import type { Warehouse } from "../types/warehouse";

import "./WarehousesPage.css";

interface Props {
    credentials: Credentials | null;
}

export default function WarehousesPage({ credentials }: Props) {
    const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
    const [loading, setLoading] = useState(false);
    const [saving, setSaving] = useState(false);
    const [deletingId, setDeletingId] = useState<number | null>(null);

    const [editingId, setEditingId] = useState<number | null>(null);
    const [name, setName] = useState("");
    const [description, setDescription] = useState("");

    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    useEffect(() => {
        if (!credentials) return;

        let active = true;

        setLoading(true);
        setError("");

        getWarehouses(credentials)
            .then((data) => {
                if (active) setWarehouses(data);
            })
            .catch((err: unknown) => {
                if (active) {
                    setError(
                        err instanceof Error
                            ? err.message
                            : "Failed to load warehouses."
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

    function resetForm() {
        setEditingId(null);
        setName("");
        setDescription("");
    }

    function startEditing(warehouse: Warehouse) {
        setEditingId(warehouse.id);
        setName(warehouse.name);
        setDescription(warehouse.description ?? "");
        setError("");
        setSuccess("");
    }

    async function handleSubmit(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        if (!credentials || saving) return;

        const trimmedName = name.trim();

        if (!trimmedName || trimmedName.length > 150) {
            setError("Warehouse name must contain 1–150 characters.");
            return;
        }

        setSaving(true);
        setError("");
        setSuccess("");

        try {
            const payload = {
                name: trimmedName,
                description: description.trim(),
            };

            if (editingId !== null) {
                await updateWarehouse(editingId, payload, credentials);
                setSuccess("Warehouse updated successfully.");
            } else {
                await createWarehouse(payload, credentials);
                setSuccess("Warehouse created successfully.");
            }

            const updated = await getWarehouses(credentials);
            setWarehouses(updated);
            resetForm();
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Failed to save warehouse."
            );
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(warehouse: Warehouse) {
        if (!credentials || deletingId !== null) return;

        const confirmed = window.confirm(
            `Delete warehouse "${warehouse.name}" (ID ${warehouse.id})?\n` +
            "This action cannot be undone."
        );

        if (!confirmed) return;

        setDeletingId(warehouse.id);
        setError("");
        setSuccess("");

        try {
            await deleteWarehouse(warehouse.id, credentials);

            const updated = await getWarehouses(credentials);
            setWarehouses(updated);

            if (editingId === warehouse.id) {
                resetForm();
            }

            setSuccess("Warehouse deleted successfully.");
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Failed to delete warehouse."
            );
        } finally {
            setDeletingId(null);
        }
    }

    if (!credentials) {
        return (
            <section className="warehouse-panel">
                <h2>Authentication required</h2>
                <p>
                    Open Route Planner and provide your manager credentials
                    before managing warehouses.
                </p>
            </section>
        );
    }

    return (
        <div className="warehouse-page">
            <div className="page-heading">
                <span className="eyebrow">WAREHOUSE MANAGEMENT</span>
                <h1>Warehouses</h1>
                <p>
                    Manage warehouse records through the Spring Boot REST API.
                    Manager permissions are required.
                </p>
            </div>

            <div className="warehouse-summary">
                <div className="warehouse-stat">
                    <span>Total warehouses</span>
                    <strong>{warehouses.length}</strong>
                </div>

                <div className="warehouse-stat">
                    <span>Data source</span>
                    <strong className="warehouse-stat-text">PostgreSQL</strong>
                </div>

                <div className="warehouse-stat">
                    <span>Access level</span>
                    <strong className="warehouse-stat-text">Manager</strong>
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

            <div className="warehouse-layout">
                <section className="warehouse-panel">
                    <div className="warehouse-panel-header">
                        <div>
                            <span className="eyebrow">INVENTORY STRUCTURE</span>
                            <h2>Warehouse list</h2>
                        </div>

                        <button
                            type="button"
                            className="warehouse-secondary-btn"
                            disabled={loading || saving || deletingId !== null}
                            onClick={async () => {
                                setLoading(true);
                                setError("");

                                try {
                                    const data = await getWarehouses(credentials);
                                    setWarehouses(data);
                                } catch (err) {
                                    setError(
                                        err instanceof Error
                                            ? err.message
                                            : "Failed to refresh warehouses."
                                    );
                                } finally {
                                    setLoading(false);
                                }
                            }}
                        >
                            Refresh
                        </button>
                    </div>

                    {loading ? (
                        <div className="warehouse-empty" role="status">
                            Loading warehouses...
                        </div>
                    ) : warehouses.length === 0 ? (
                        <div className="warehouse-empty">
                            No warehouses to display.
                        </div>
                    ) : (
                        <div className="warehouse-table-wrapper">
                            <table className="warehouse-table">
                                <thead>
                                <tr>
                                    <th>ID</th>
                                    <th>Warehouse</th>
                                    <th>Description</th>
                                    <th>Actions</th>
                                </tr>
                                </thead>

                                <tbody>
                                {warehouses.map((warehouse) => (
                                    <tr key={warehouse.id}>
                                        <td>
                        <span className="warehouse-id">
                          #{warehouse.id}
                        </span>
                                        </td>

                                        <td>
                                            <strong>{warehouse.name}</strong>
                                        </td>

                                        <td>
                                            {warehouse.description || "No description"}
                                        </td>

                                        <td>
                                            <div className="warehouse-actions">
                                                <button
                                                    type="button"
                                                    disabled={saving || deletingId !== null}
                                                    onClick={() => startEditing(warehouse)}
                                                >
                                                    Edit
                                                </button>

                                                <button
                                                    type="button"
                                                    className="warehouse-delete"
                                                    disabled={saving || deletingId !== null}
                                                    onClick={() => handleDelete(warehouse)}
                                                >
                                                    {deletingId === warehouse.id
                                                        ? "Deleting..."
                                                        : "Delete"}
                                                </button>
                                            </div>
                                        </td>
                                    </tr>
                                ))}
                                </tbody>
                            </table>
                        </div>
                    )}
                </section>

                <section className="warehouse-panel">
                    <div className="warehouse-panel-header">
                        <div>
              <span className="eyebrow">
                {editingId !== null ? "EDIT RECORD" : "NEW RECORD"}
              </span>

                            <h2>
                                {editingId !== null
                                    ? `Edit warehouse #${editingId}`
                                    : "Add warehouse"}
                            </h2>
                        </div>
                    </div>

                    <form className="warehouse-form" onSubmit={handleSubmit}>
                        <label htmlFor="warehouse-name">
                            Warehouse name
                        </label>

                        <input
                            id="warehouse-name"
                            value={name}
                            maxLength={150}
                            required
                            placeholder="e.g. Main Distribution Center"
                            onChange={(event) => setName(event.target.value)}
                        />

                        <label htmlFor="warehouse-description">
                            Description
                        </label>

                        <textarea
                            id="warehouse-description"
                            value={description}
                            maxLength={1000}
                            rows={5}
                            placeholder="Describe the warehouse..."
                            onChange={(event) => setDescription(event.target.value)}
                        />

                        <button
                            className="primary-button"
                            type="submit"
                            disabled={saving || deletingId !== null}
                        >
                            {saving
                                ? "Saving..."
                                : editingId !== null
                                    ? "Save changes"
                                    : "Create warehouse"}
                        </button>

                        {editingId !== null && (
                            <button
                                type="button"
                                className="warehouse-secondary-btn"
                                onClick={resetForm}
                                disabled={saving}
                            >
                                Cancel editing
                            </button>
                        )}
                    </form>

                    <p className="warehouse-help">
                        Changes are saved to PostgreSQL through the
                        Spring Boot API. Deleting a warehouse with related
                        records may be blocked by database constraints.
                    </p>
                </section>
            </div>
        </div>
    );
}
