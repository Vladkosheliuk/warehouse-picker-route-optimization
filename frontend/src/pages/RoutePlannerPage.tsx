import { useEffect, useMemo, useState } from "react";
import type { Credentials } from "../api/client";
import {
    calculateTaskRoute,
    listMyTasks,
    listTasks,
} from "../api/pickingTasks";
import type { PickingTask } from "../api/pickingTasks";
import type {
    RouteComparison,
    RouteResult,
} from "../types/route";

import "./RoutePlannerPage.css";

type Role = "MANAGER" | "PICKER";

interface Props {
    credentials: Credentials;
    role: Role;
    onUnauthorized?: () => void;
}

function formatMetric(value: number) {
    return new Intl.NumberFormat("en-US", {
        maximumFractionDigits: 2,
    }).format(value);
}

function percentDifference(
    optimized: number,
    baseline: number
) {
    if (baseline <= 0) return null;
    return ((baseline - optimized) / baseline) * 100;
}

function formatStatus(status: string) {
    return status.replaceAll("_", " ");
}

function isUsable(task: PickingTask, role: Role) {
    if (
        task.status === "CANCELLED" ||
        task.status === "COMPLETED"
    ) {
        return false;
    }

    if (role === "PICKER") {
        return (
            task.status === "ASSIGNED" ||
            task.status === "IN_PROGRESS"
        );
    }

    return true;
}

function AlgorithmSummary({
                              title,
                              description,
                              route,
                              baseline,
                              primary,
                              onSelect,
                              selected,
                          }: {
    title: string;
    description: string;
    route: RouteResult;
    baseline: RouteResult;
    primary: boolean;
    selected: boolean;
    onSelect: () => void;
}) {
    const maxDistance = Math.max(
        route.totalDistanceM,
        baseline.totalDistanceM,
        1
    );

    const width =
        (route.totalDistanceM / maxDistance) * 100;

    return (
        <button
            type="button"
            className={`rp-algorithm ${
                selected ? "rp-algorithm--selected" : ""
            }`}
            onClick={onSelect}
            aria-pressed={selected}
        >
            <div className="rp-algorithm__top">
        <span className="rp-algorithm__name">
          {title}
        </span>

                <span className="rp-algorithm__tag">
          {primary ? "WEIGHT-AWARE" : "BASELINE"}
        </span>
            </div>

            <p>{description}</p>

            <strong>
                {formatMetric(route.totalDistanceM)}{" "}
                <small>m</small>
            </strong>

            <div
                className="rp-algorithm__track"
                aria-hidden="true"
            >
                <span style={{ width: `${width}%` }} />
            </div>

            <div className="rp-algorithm__bottom">
                <span>Walking distance</span>
                <b>
                    {formatMetric(route.loadDistanceKgM)} kg·m
                </b>
            </div>
        </button>
    );
}

function RouteJourney({
                          route,
                          task,
                      }: {
    route: RouteResult;
    task: PickingTask;
}) {
    const names = new Map(
        task.items.map((item) => [
            item.productId,
            item.productName,
        ])
    );

    return (
        <>
            <div
                className="rp-journey"
                aria-label="Picking sequence"
            >
                <div className="rp-journey__start">
          <span className="rp-journey__dot">
            S
          </span>
                    <span>Start</span>
                </div>

                {route.stops.map((stop, index) => {
                    const exit = stop.productId === null;

                    return (
                        <div
                            className="rp-journey__step"
                            key={`${index}-${stop.nodeId}`}
                        >
              <span
                  className="rp-journey__connector"
                  aria-hidden="true"
              />
                            <span
                                className={`rp-journey__dot ${
                                    exit ? "rp-journey__dot--exit" : ""
                                }`}
                            >
                {exit ? "✓" : index + 1}
              </span>
                            <span>
                {exit ? "Exit" : `Pick ${index + 1}`}
              </span>
                        </div>
                    );
                })}
            </div>

            <div className="rp-picking-list">
                {route.stops.map((stop, index) => {
                    const exit = stop.productId === null;

                    const name = exit
                        ? "Warehouse exit"
                        : names.get(stop.productId!) ??
                        `Product #${stop.productId}`;

                    return (
                        <div
                            className="rp-picking-stop"
                            key={`${index}-${stop.nodeId}`}
                        >
              <span
                  className={`rp-picking-stop__number ${
                      exit
                          ? "rp-picking-stop__number--exit"
                          : ""
                  }`}
              >
                {exit ? "✓" : index + 1}
              </span>

                            <div className="rp-picking-stop__body">
                                <strong>{name}</strong>
                                <span>
                  {exit
                      ? "Finish picking"
                      : `Pick ${stop.quantity} unit${
                          stop.quantity === 1 ? "" : "s"
                      }`}
                                    {` · Location node #${stop.nodeId}`}
                </span>
                            </div>

                            <div className="rp-picking-stop__distance">
                                <strong>
                                    {formatMetric(
                                        stop.distanceFromPreviousM
                                    )} m
                                </strong>
                                <span>from previous</span>
                            </div>
                        </div>
                    );
                })}
            </div>

            <details className="rp-path-details">
                <summary>
                    Technical details: all traversed nodes
                </summary>

                <div className="rp-path-nodes">
                    {route.fullPathNodeIds.map((id, index) => (
                        <span key={`${id}-${index}`}>
              {index > 0 ? " → " : ""}
                            {id}
            </span>
                    ))}
                </div>
            </details>
        </>
    );
}

export default function RoutePlannerPage({
                                             credentials,
                                             role,
                                             onUnauthorized,
                                         }: Props) {
    const [tasks, setTasks] =
        useState<PickingTask[]>([]);

    const [selectedId, setSelectedId] =
        useState<number | null>(null);

    const [query, setQuery] = useState("");
    const [loadingTasks, setLoadingTasks] =
        useState(true);

    const [calculating, setCalculating] =
        useState(false);

    const [error, setError] = useState("");

    const [result, setResult] =
        useState<RouteComparison | null>(null);

    const [algorithm, setAlgorithm] =
        useState<"optimized" | "sShape">(
            "optimized"
        );

    useEffect(() => {
        let active = true;

        setLoadingTasks(true);
        setError("");

        const load =
            role === "MANAGER"
                ? listTasks(credentials)
                : listMyTasks(credentials);

        void load
            .then((items) => {
                if (!active) return;

                setTasks(items);

                setSelectedId((current) =>
                    items.some(
                        (task) =>
                            task.id === current &&
                            isUsable(task, role)
                    )
                        ? current
                        : items.find((task) =>
                        isUsable(task, role)
                    )?.id ?? null
                );
            })
            .catch((cause: unknown) => {
                if (!active) return;

                const message =
                    cause instanceof Error
                        ? cause.message
                        : "Could not load picking tasks.";

                if (
                    message === "Invalid username or password"
                ) {
                    onUnauthorized?.();
                } else {
                    setError(message);
                }
            })
            .finally(() => {
                if (active) {
                    setLoadingTasks(false);
                }
            });

        return () => {
            active = false;
        };
    }, [credentials, role, onUnauthorized]);

    const availableTasks = useMemo(
        () =>
            tasks.filter((task) =>
                isUsable(task, role)
            ),
        [tasks, role]
    );

    const matchingTasks = useMemo(() => {
        const term = query.trim().toLowerCase();

        if (!term) {
            return availableTasks;
        }

        return availableTasks.filter((task) => {
            const searchable = [
                String(task.id),
                task.warehouseName,
                task.assignedPickerUsername ?? "",
                ...task.items.flatMap((item) => [
                    item.productName,
                    item.sku,
                ]),
            ]
                .join(" ")
                .toLowerCase();

            return searchable.includes(term);
        });
    }, [availableTasks, query]);

    const selectedTask = availableTasks.find(
        (task) => task.id === selectedId
    );

    const currentRoute = result?.[algorithm];

    const distanceDifference = result
        ? percentDifference(
            result.optimized.totalDistanceM,
            result.sShape.totalDistanceM
        )
        : null;

    const loadDifference = result
        ? percentDifference(
            result.optimized.loadDistanceKgM,
            result.sShape.loadDistanceKgM
        )
        : null;

    function selectTask(id: number) {
        if (calculating) return;

        setSelectedId(id);
        setResult(null);
        setAlgorithm("optimized");
        setError("");
    }

    async function calculate() {
        if (!selectedTask || calculating) {
            return;
        }

        setCalculating(true);
        setResult(null);
        setError("");

        try {
            const comparison =
                await calculateTaskRoute(
                    selectedTask.id,
                    credentials
                );

            setResult(comparison);
            setAlgorithm("optimized");
        } catch (cause) {
            const message =
                cause instanceof Error
                    ? cause.message
                    : "Unable to calculate route.";

            if (
                message === "Invalid username or password"
            ) {
                onUnauthorized?.();
            } else {
                setError(message);
            }
        } finally {
            setCalculating(false);
        }
    }

    return (
        <div className="rp-page">
            <header className="page-heading">
        <span className="eyebrow">
          SMART PICKING
        </span>

                <h1>Route Planner</h1>

                <p>
                    Select a picking order, build its route
                    and follow the recommended sequence
                    of product locations.
                </p>
            </header>

            <div className="rp-layout">
                <section className="rp-panel rp-orders">
                    <div className="rp-section-heading">
                        <div>
              <span className="eyebrow">
                STEP 01
              </span>
                            <h2>
                                {role === "MANAGER"
                                    ? "Choose an order"
                                    : "My active orders"}
                            </h2>
                        </div>

                        <span className="rp-count">
              {availableTasks.length}
            </span>
                    </div>

                    <label htmlFor="rp-search">
                        Find a picking order
                    </label>

                    <input
                        id="rp-search"
                        className="rp-search"
                        type="search"
                        placeholder="Search by product, warehouse or employee"
                        value={query}
                        onChange={(event) =>
                            setQuery(event.target.value)
                        }
                    />

                    {loadingTasks ? (
                        <div className="rp-empty">
                            Loading orders...
                        </div>
                    ) : availableTasks.length === 0 ? (
                        <div className="rp-empty">
                            {role === "MANAGER"
                                ? "No active orders. Create one in Picking Tasks."
                                : "You have no assigned orders yet."}
                        </div>
                    ) : matchingTasks.length === 0 ? (
                        <div className="rp-empty">
                            No orders match your search.
                        </div>
                    ) : (
                        <div className="rp-order-list">
                            {matchingTasks.map((task) => (
                                <button
                                    type="button"
                                    key={task.id}
                                    className={`rp-order ${
                                        selectedId === task.id
                                            ? "rp-order--selected"
                                            : ""
                                    }`}
                                    onClick={() =>
                                        selectTask(task.id)
                                    }
                                    disabled={calculating}
                                >
                                    <div className="rp-order__header">
                                        <strong>
                                            Order #{task.id}
                                        </strong>
                                        <span>
                      {formatStatus(task.status)}
                    </span>
                                    </div>

                                    <p>{task.warehouseName}</p>

                                    {role === "MANAGER" && (
                                        <small>
                                            Assigned:{" "}
                                            {task.assignedPickerUsername ??
                                                "Not assigned"}
                                        </small>
                                    )}

                                    <div className="rp-order__products">
                                        {task.items
                                            .slice(0, 3)
                                            .map((item) => (
                                                <span key={item.id}>
                          {item.productName} ×{" "}
                                                    {item.requiredQuantity}
                        </span>
                                            ))}

                                        {task.items.length > 3 && (
                                            <span>
                        +{task.items.length - 3} more
                      </span>
                                        )}
                                    </div>
                                </button>
                            ))}
                        </div>
                    )}
                </section>

                <div className="rp-right">
                    <section className="rp-panel rp-selected">
                        <div className="rp-section-heading">
                            <div>
                <span className="eyebrow">
                  STEP 02
                </span>
                                <h2>Build the route</h2>
                            </div>
                        </div>

                        {selectedTask ? (
                            <>
                                <div className="rp-selected__meta">
                                    <div>
                                        <span>Selected order</span>
                                        <strong>
                                            #{selectedTask.id}
                                        </strong>
                                    </div>

                                    <div>
                                        <span>Warehouse</span>
                                        <strong>
                                            {selectedTask.warehouseName}
                                        </strong>
                                    </div>

                                    <div>
                                        <span>Products</span>
                                        <strong>
                                            {selectedTask.items.length} types
                                        </strong>
                                    </div>
                                </div>

                                <button
                                    type="button"
                                    className="rp-calculate"
                                    onClick={() => void calculate()}
                                    disabled={
                                        calculating || loadingTasks
                                    }
                                >
                                    {calculating
                                        ? "Calculating route..."
                                        : "Calculate optimal route →"}
                                </button>

                                <p className="rp-helper">
                                    We compare a weight-aware heuristic
                                    with a standard S-shape route.
                                    The recommended route is calculated
                                    from the warehouse graph.
                                </p>
                            </>
                        ) : (
                            <div className="rp-empty">
                                Select an order on the left
                                to continue.
                            </div>
                        )}
                    </section>

                    {error && (
                        <div
                            className="rp-error"
                            role="alert"
                        >
                            {error}
                        </div>
                    )}

                    {result && selectedTask && (
                        <section className="rp-panel rp-results">
                            <div className="rp-section-heading">
                                <div>
                  <span className="eyebrow">
                    STEP 03 · RESULTS
                  </span>

                                    <h2>Route comparison</h2>
                                </div>

                                <span className="rp-ready">
                  Ready
                </span>
                            </div>

                            <div className="rp-impact">
                                <div>
                  <span>
                    Walking distance versus S-shape
                  </span>

                                    <strong>
                                        {distanceDifference === null
                                            ? "N/A"
                                            : `${
                                                distanceDifference > 0
                                                    ? "−"
                                                    : distanceDifference < 0
                                                        ? "+"
                                                        : ""
                                            }${formatMetric(
                                                Math.abs(distanceDifference)
                                            )}%`}
                                    </strong>
                                </div>

                                <div>
                  <span>
                    Load-distance versus S-shape
                  </span>

                                    <strong>
                                        {loadDifference === null
                                            ? "N/A"
                                            : `${
                                                loadDifference > 0
                                                    ? "−"
                                                    : loadDifference < 0
                                                        ? "+"
                                                        : ""
                                            }${formatMetric(
                                                Math.abs(loadDifference)
                                            )}%`}
                                    </strong>
                                </div>
                            </div>

                            <div className="rp-algorithms">
                                <AlgorithmSummary
                                    title="Optimized route"
                                    description="Weight-aware picking order"
                                    route={result.optimized}
                                    baseline={result.sShape}
                                    selected={
                                        algorithm === "optimized"
                                    }
                                    primary
                                    onSelect={() =>
                                        setAlgorithm("optimized")
                                    }
                                />

                                <AlgorithmSummary
                                    title="S-shape route"
                                    description="Standard warehouse traversal"
                                    route={result.sShape}
                                    baseline={result.optimized}
                                    selected={
                                        algorithm === "sShape"
                                    }
                                    primary={false}
                                    onSelect={() =>
                                        setAlgorithm("sShape")
                                    }
                                />
                            </div>

                            {currentRoute && (
                                <div className="rp-route-detail">
                                    <div className="rp-section-heading">
                                        <div>
                      <span className="eyebrow">
                        PICKING DIRECTIONS
                      </span>

                                            <h3>
                                                {algorithm === "optimized"
                                                    ? "Recommended picking order"
                                                    : "S-shape picking order"}
                                            </h3>
                                        </div>

                                        <span className="rp-count">
                      {currentRoute.stops.length} stops
                    </span>
                                    </div>

                                    <RouteJourney
                                        route={currentRoute}
                                        task={selectedTask}
                                    />
                                </div>
                            )}

                            <p className="rp-helper">
                                This is a sequence diagram,
                                not a floor-plan overlay.
                                Exact highlighting on the warehouse
                                map requires node IDs and coordinates
                                to be returned together by the API.
                            </p>
                        </section>
                    )}
                </div>
            </div>
        </div>
    );
}
