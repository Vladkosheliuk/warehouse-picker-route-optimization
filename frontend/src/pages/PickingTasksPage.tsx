import { useEffect, useState } from "react";
import type { FormEvent } from "react";
import type { Credentials } from "../api/client";
import type { Warehouse } from "../types/warehouse";
import type { RouteComparison } from "../types/route";
import type { WarehouseUser } from "../api/users";

import { getWarehouses } from "../api/warehouses";
import { getUsers } from "../api/users";

import {
    assignPickingTask,
    calculateTaskRoute,
    cancelPickingTask,
    collectPickingItem,
    completePickingTask,
    createPickingTask,
    listAvailableProducts,
    listMyTasks,
    listTasks,
    startPickingTask,
} from "../api/pickingTasks";

import type {
    PickingTask,
    ProductOption,
} from "../api/pickingTasks";

import "./PickingTasksPage.css";

interface Props {
    credentials: Credentials;
    role: "MANAGER" | "PICKER";
}

interface DraftItem {
    productId: string;
    quantity: string;
}

export default function PickingTasksPage({
                                             credentials,
                                             role,
                                         }: Props) {
    const manager = role === "MANAGER";

    const [tasks, setTasks] = useState<PickingTask[]>([]);
    const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
    const [pickers, setPickers] = useState<WarehouseUser[]>([]);
    const [products, setProducts] = useState<ProductOption[]>([]);

    const [warehouseId, setWarehouseId] = useState("");
    const [pickerId, setPickerId] = useState("");

    const [draftItems, setDraftItems] = useState<DraftItem[]>([
        { productId: "", quantity: "1" },
    ]);

    const [assignedTo, setAssignedTo] =
        useState<Record<number, string>>({});

    const [collectedAmounts, setCollectedAmounts] =
        useState<Record<number, string>>({});

    const [route, setRoute] = useState<RouteComparison | null>(null);
    const [routeTaskId, setRouteTaskId] = useState<number | null>(null);

    const [loading, setLoading] = useState(true);
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");

    async function refreshTasks() {
        const result = manager
            ? await listTasks(credentials)
            : await listMyTasks(credentials);

        setTasks(result);
    }

    useEffect(() => {
        let active = true;

        const work = async () => {
            try {
                const result = manager
                    ? await listTasks(credentials)
                    : await listMyTasks(credentials);

                if (active) setTasks(result);

                if (manager) {
                    const [warehouseData, userData] = await Promise.all([
                        getWarehouses(credentials),
                        getUsers(credentials),
                    ]);

                    if (!active) return;

                    setWarehouses(warehouseData);
                    setPickers(
                        userData.filter(
                            (user) =>
                                user.role === "PICKER" && user.active
                        )
                    );

                    if (warehouseData.length > 0) {
                        setWarehouseId(String(warehouseData[0].id));
                    }

                    const firstPicker = userData.find(
                        (user) =>
                            user.role === "PICKER" && user.active
                    );

                    if (firstPicker) {
                        setPickerId(String(firstPicker.id));
                    }
                }
            } catch (cause) {
                if (active) {
                    setError(
                        cause instanceof Error
                            ? cause.message
                            : "Unable to load picking tasks."
                    );
                }
            } finally {
                if (active) setLoading(false);
            }
        };

        void work();

        return () => {
            active = false;
        };
    }, [credentials, manager]);

    useEffect(() => {
        if (!manager || !warehouseId) {
            setProducts([]);
            return;
        }

        let active = true;

        listAvailableProducts(
            Number(warehouseId),
            credentials
        )
            .then((result) => {
                if (active) setProducts(result);
            })
            .catch((cause: unknown) => {
                if (active) {
                    setProducts([]);
                    setError(
                        cause instanceof Error
                            ? cause.message
                            : "Unable to load available products."
                    );
                }
            });

        return () => {
            active = false;
        };
    }, [warehouseId, manager, credentials]);

    async function runAction(
        action: () => Promise<PickingTask>,
        message: string
    ) {
        if (busy) return;

        setBusy(true);
        setError("");
        setSuccess("");

        try {
            const updated = await action();

            setTasks((previous) =>
                previous.map((task) =>
                    task.id === updated.id ? updated : task
                )
            );

            setRoute(null);
            setRouteTaskId(null);
            setSuccess(message);
        } catch (cause) {
            setError(
                cause instanceof Error
                    ? cause.message
                    : "Operation failed."
            );
        } finally {
            setBusy(false);
        }
    }

    async function handleCreate(
        event: FormEvent<HTMLFormElement>
    ) {
        event.preventDefault();

        if (busy) return;

        const selectedWarehouseId = Number(warehouseId);
        const selectedPickerId = Number(pickerId);

        const lines = draftItems.map((item) => ({
            productId: Number(item.productId),
            quantity: Number(item.quantity),
        }));

        if (
            !Number.isSafeInteger(selectedWarehouseId) ||
            selectedWarehouseId <= 0 ||
            !Number.isSafeInteger(selectedPickerId) ||
            selectedPickerId <= 0 ||
            lines.some(
                (line) =>
                    !Number.isSafeInteger(line.productId) ||
                    line.productId <= 0 ||
                    !Number.isSafeInteger(line.quantity) ||
                    line.quantity <= 0
            )
        ) {
            setError("Select a warehouse, picker and valid products.");
            return;
        }

        if (
            new Set(lines.map((line) => line.productId)).size
            !== lines.length
        ) {
            setError("Each product can appear only once.");
            return;
        }

        setBusy(true);
        setError("");
        setSuccess("");

        try {
            const created = await createPickingTask(
                {
                    warehouseId: selectedWarehouseId,
                    pickerId: selectedPickerId,
                    items: lines,
                },
                credentials
            );

            await refreshTasks();

            setDraftItems([{ productId: "", quantity: "1" }]);
            setSuccess(
                `Picking task #${created.id} created and assigned.`
            );
        } catch (cause) {
            setError(
                cause instanceof Error
                    ? cause.message
                    : "Unable to create picking task."
            );
        } finally {
            setBusy(false);
        }
    }

    async function handleRoute(id: number) {
        if (busy) return;

        setBusy(true);
        setError("");
        setSuccess("");

        try {
            const result = await calculateTaskRoute(
                id,
                credentials
            );

            setRoute(result);
            setRouteTaskId(id);
        } catch (cause) {
            setRoute(null);
            setError(
                cause instanceof Error
                    ? cause.message
                    : "Cannot calculate route."
            );
        } finally {
            setBusy(false);
        }
    }

    function updateDraftItem(
        index: number,
        field: keyof DraftItem,
        value: string
    ) {
        setDraftItems((previous) =>
            previous.map((item, i) =>
                i === index ? { ...item, [field]: value } : item
            )
        );
    }

    const activeTasks = tasks.filter(
        (task) =>
            task.status === "ASSIGNED" ||
            task.status === "IN_PROGRESS"
    ).length;

    const completedTasks = tasks.filter(
        (task) => task.status === "COMPLETED"
    ).length;

    return (
        <div className="tasks-page">
            <div className="page-heading">
        <span className="eyebrow">
          {manager ? "MANAGER WORKSPACE" : "PICKER WORKSPACE"}
        </span>

                <h1>
                    {manager ? "Picking Tasks" : "My Picking Tasks"}
                </h1>

                <p>
                    {manager
                        ? "Create orders and assign warehouse employees."
                        : "View assigned orders, collect products and complete tasks."}
                </p>
            </div>

            <div className="tasks-stats">
                <div className="tasks-stat">
                    <span>Total tasks</span>
                    <strong>{tasks.length}</strong>
                </div>

                <div className="tasks-stat">
                    <span>Active tasks</span>
                    <strong>{activeTasks}</strong>
                </div>

                <div className="tasks-stat">
                    <span>Completed</span>
                    <strong>{completedTasks}</strong>
                </div>
            </div>

            {error && (
                <div className="tasks-alert error" role="alert">
                    {error}
                </div>
            )}

            {success && (
                <div className="tasks-alert success" role="status">
                    {success}
                </div>
            )}

            {manager && (
                <section className="tasks-panel">
                    <span className="eyebrow">CREATE NEW TASK</span>
                    <h2>Assign a picking order</h2>

                    <form className="tasks-form" onSubmit={handleCreate}>
                        <label htmlFor="task-warehouse">
                            Warehouse
                        </label>

                        <select
                            id="task-warehouse"
                            value={warehouseId}
                            onChange={(event) => {
                                setWarehouseId(event.target.value);
                                setDraftItems([
                                    { productId: "", quantity: "1" },
                                ]);
                            }}
                            required
                        >
                            {warehouses.map((warehouse) => (
                                <option
                                    key={warehouse.id}
                                    value={warehouse.id}
                                >
                                    #{warehouse.id} — {warehouse.name}
                                </option>
                            ))}
                        </select>

                        <label htmlFor="task-picker">
                            Assign to picker
                        </label>

                        <select
                            id="task-picker"
                            value={pickerId}
                            onChange={(event) =>
                                setPickerId(event.target.value)
                            }
                            required
                        >
                            {pickers.map((picker) => (
                                <option key={picker.id} value={picker.id}>
                                    {picker.fullName} ({picker.username})
                                </option>
                            ))}
                        </select>

                        <h3>Products to collect</h3>

                        {products.length === 0 && (
                            <p className="tasks-note">
                                No stocked products found in this warehouse.
                                Choose Demo Warehouse or add inventory first.
                            </p>
                        )}

                        {draftItems.map((item, index) => (
                            <div className="tasks-line-editor" key={index}>
                                <select
                                    aria-label={`Product ${index + 1}`}
                                    value={item.productId}
                                    onChange={(event) =>
                                        updateDraftItem(
                                            index,
                                            "productId",
                                            event.target.value
                                        )
                                    }
                                    required
                                >
                                    <option value="">Select product</option>
                                    {products.map((product) => (
                                        <option
                                            key={product.id}
                                            value={product.id}
                                        >
                                            {product.sku} — {product.name}
                                            {" "}({product.availableQuantity} in stock)
                                        </option>
                                    ))}
                                </select>

                                <input
                                    aria-label={`Quantity ${index + 1}`}
                                    type="number"
                                    min="1"
                                    step="1"
                                    value={item.quantity}
                                    onChange={(event) =>
                                        updateDraftItem(
                                            index,
                                            "quantity",
                                            event.target.value
                                        )
                                    }
                                    required
                                />

                                <button
                                    type="button"
                                    onClick={() =>
                                        setDraftItems((previous) =>
                                            previous.filter((_, i) => i !== index)
                                        )
                                    }
                                    disabled={draftItems.length === 1}
                                >
                                    Remove
                                </button>
                            </div>
                        ))}

                        <div className="tasks-actions">
                            <button
                                type="button"
                                className="tasks-secondary"
                                onClick={() =>
                                    setDraftItems((previous) => [
                                        ...previous,
                                        { productId: "", quantity: "1" },
                                    ])
                                }
                                disabled={products.length === 0}
                            >
                                + Add product
                            </button>

                            <button
                                type="submit"
                                className="primary-button"
                                disabled={
                                    busy ||
                                    loading ||
                                    !warehouseId ||
                                    !pickerId ||
                                    products.length === 0
                                }
                            >
                                {busy ? "Saving..." : "Create picking task"}
                            </button>
                        </div>
                    </form>
                </section>
            )}

            <section className="tasks-panel">
                <div className="tasks-panel-header">
                    <div>
            <span className="eyebrow">
              {manager ? "ALL ORDERS" : "MY ASSIGNED ORDERS"}
            </span>
                        <h2>Task list</h2>
                    </div>

                    <button
                        type="button"
                        className="tasks-secondary"
                        disabled={busy || loading}
                        onClick={() => {
                            setLoading(true);
                            setError("");

                            void refreshTasks()
                                .catch((cause: unknown) =>
                                    setError(
                                        cause instanceof Error
                                            ? cause.message
                                            : "Unable to refresh tasks."
                                    )
                                )
                                .finally(() => setLoading(false));
                        }}
                    >
                        Refresh
                    </button>
                </div>

                {loading ? (
                    <p className="tasks-note">Loading tasks...</p>
                ) : tasks.length === 0 ? (
                    <p className="tasks-note">
                        {manager
                            ? "No picking tasks have been created."
                            : "There are no tasks assigned to your account."}
                    </p>
                ) : (
                    <div className="tasks-list">
                        {tasks.map((task) => {
                            const complete = task.items.length > 0 &&
                                task.items.every(
                                    (item) =>
                                        item.collectedQuantity ===
                                        item.requiredQuantity
                                );

                            return (
                                <article className="tasks-card" key={task.id}>
                                    <div className="tasks-card-header">
                                        <div>
                                            <h3>Task #{task.id}</h3>
                                            <p>
                                                {task.warehouseName}
                                                {" · "}
                                                {task.assignedPickerUsername ??
                                                    "Unassigned"}
                                            </p>
                                        </div>

                                        <span className="tasks-status">
                      {task.status.replaceAll("_", " ")}
                    </span>
                                    </div>

                                    <div className="tasks-products">
                                        {task.items.map((item) => {
                                            const remaining =
                                                item.requiredQuantity -
                                                item.collectedQuantity;

                                            return (
                                                <div
                                                    className="tasks-product"
                                                    key={item.id}
                                                >
                                                    <div>
                                                        <strong>{item.productName}</strong>
                                                        <span>
                              {item.sku} · Collected{" "}
                                                            {item.collectedQuantity}/
                                                            {item.requiredQuantity}
                            </span>
                                                    </div>

                                                    {!manager &&
                                                        task.status === "IN_PROGRESS" &&
                                                        remaining > 0 && (
                                                            <div className="tasks-collect">
                                                                <input
                                                                    type="number"
                                                                    min="1"
                                                                    max={remaining}
                                                                    step="1"
                                                                    aria-label={`Collect ${item.sku}`}
                                                                    value={
                                                                        collectedAmounts[item.id] ?? "1"
                                                                    }
                                                                    onChange={(event) =>
                                                                        setCollectedAmounts(
                                                                            (previous) => ({
                                                                                ...previous,
                                                                                [item.id]: event.target.value,
                                                                            })
                                                                        )
                                                                    }
                                                                />

                                                                <button
                                                                    type="button"
                                                                    disabled={busy}
                                                                    onClick={() => {
                                                                        const amount = Number(
                                                                            collectedAmounts[item.id] ?? "1"
                                                                        );

                                                                        if (
                                                                            !Number.isSafeInteger(amount) ||
                                                                            amount < 1 ||
                                                                            amount > remaining
                                                                        ) {
                                                                            setError(
                                                                                "Enter a valid collected quantity."
                                                                            );
                                                                            return;
                                                                        }

                                                                        void runAction(
                                                                            () => collectPickingItem(
                                                                                task.id,
                                                                                item.id,
                                                                                amount,
                                                                                credentials
                                                                            ),
                                                                            `${item.sku}: collected ${amount}`
                                                                        );
                                                                    }}
                                                                >
                                                                    Collect
                                                                </button>
                                                            </div>
                                                        )}
                                                </div>
                                            );
                                        })}
                                    </div>

                                    <div className="tasks-actions">
                                        {manager && task.status === "NEW" && (
                                            <>
                                                <select
                                                    aria-label={`Assign task ${task.id}`}
                                                    value={assignedTo[task.id] ?? ""}
                                                    onChange={(event) =>
                                                        setAssignedTo((previous) => ({
                                                            ...previous,
                                                            [task.id]: event.target.value,
                                                        }))
                                                    }
                                                >
                                                    <option value="">Choose picker</option>
                                                    {pickers.map((picker) => (
                                                        <option
                                                            key={picker.id}
                                                            value={picker.id}
                                                        >
                                                            {picker.username}
                                                        </option>
                                                    ))}
                                                </select>

                                                <button
                                                    type="button"
                                                    className="tasks-secondary"
                                                    disabled={busy || !assignedTo[task.id]}
                                                    onClick={() =>
                                                        void runAction(
                                                            () => assignPickingTask(
                                                                task.id,
                                                                Number(assignedTo[task.id]),
                                                                credentials
                                                            ),
                                                            `Task #${task.id} assigned.`
                                                        )
                                                    }
                                                >
                                                    Assign
                                                </button>
                                            </>
                                        )}

                                        {!manager &&
                                            task.status === "ASSIGNED" && (
                                                <button
                                                    type="button"
                                                    className="primary-button"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        void runAction(
                                                            () => startPickingTask(
                                                                task.id,
                                                                credentials
                                                            ),
                                                            `Task #${task.id} started.`
                                                        )
                                                    }
                                                >
                                                    Start task
                                                </button>
                                            )}

                                        {(task.status === "ASSIGNED" ||
                                            task.status === "IN_PROGRESS") && (
                                            <button
                                                type="button"
                                                className="tasks-secondary"
                                                disabled={busy}
                                                onClick={() =>
                                                    void handleRoute(task.id)
                                                }
                                            >
                                                Calculate route
                                            </button>
                                        )}

                                        {!manager &&
                                            task.status === "IN_PROGRESS" &&
                                            complete && (
                                                <button
                                                    type="button"
                                                    className="primary-button"
                                                    disabled={busy}
                                                    onClick={() =>
                                                        void runAction(
                                                            () => completePickingTask(
                                                                task.id,
                                                                credentials
                                                            ),
                                                            `Task #${task.id} completed.`
                                                        )
                                                    }
                                                >
                                                    Complete task
                                                </button>
                                            )}

                                        {manager &&
                                            task.status !== "COMPLETED" &&
                                            task.status !== "CANCELLED" && (
                                                <button
                                                    type="button"
                                                    className="tasks-danger"
                                                    disabled={busy}
                                                    onClick={() => {
                                                        if (!window.confirm(
                                                            `Cancel task #${task.id}?`
                                                        )) return;

                                                        void runAction(
                                                            () => cancelPickingTask(
                                                                task.id,
                                                                credentials
                                                            ),
                                                            `Task #${task.id} cancelled.`
                                                        );
                                                    }}
                                                >
                                                    Cancel task
                                                </button>
                                            )}
                                    </div>

                                    {route &&
                                        routeTaskId === task.id && (
                                            <div className="tasks-route">
                                                <h4>Optimized route</h4>
                                                <p>
                                                    Distance:{" "}
                                                    <strong>
                                                        {route.optimized.totalDistanceM} m
                                                    </strong>
                                                    {" · "}
                                                    Load-distance:{" "}
                                                    <strong>
                                                        {route.optimized.loadDistanceKgM}
                                                        {" "}kg·m
                                                    </strong>
                                                </p>

                                                <p>
                                                    Node path:{" "}
                                                    {route.optimized.fullPathNodeIds.join(
                                                        " → "
                                                    )}
                                                </p>

                                                <h4>Picking sequence</h4>
                                                {route.optimized.stops.map(
                                                    (stop, index) => (
                                                        <p key={`${stop.nodeId}-${index}`}>
                                                            {index + 1}.{" "}
                                                            {stop.productId === null
                                                                ? "Exit warehouse"
                                                                : `Product #${stop.productId}, quantity ${stop.quantity}`}
                                                            {" · "}Node {stop.nodeId}
                                                        </p>
                                                    )
                                                )}
                                            </div>
                                        )}
                                </article>
                            );
                        })}
                    </div>
                )}
            </section>
        </div>
    );
}
