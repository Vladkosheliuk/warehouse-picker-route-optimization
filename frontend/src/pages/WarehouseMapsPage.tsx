
import { useEffect, useRef, useState } from "react";
import type { ChangeEvent } from "react";
import type { Credentials } from "../api/client";
import { getWarehouses } from "../api/warehouses";
import { getWarehouseMap, importWarehouseMap } from "../api/warehouseMaps";
import type { Warehouse } from "../types/warehouse";
import type {
    WarehouseMap,
    WarehouseMapEdge,
    WarehouseMapNode,
    WarehouseNodeType,
} from "../types/warehouseMap";
import InteractiveWarehouseMap from "../components/InteractiveWarehouseMap";
import "./WarehouseMapsPage.css";

type Props = { credentials: Credentials };

const nodeTypes: WarehouseNodeType[] = [
    "ENTRANCE", "EXIT", "INTERSECTION", "STORAGE",
];

function isObject(value: unknown): value is Record<string, unknown> {
    return typeof value === "object" && value !== null && !Array.isArray(value);
}

function validDecimal(value: unknown): value is number {
    return typeof value === "number" && Number.isFinite(value) &&
        Math.abs(value) < 100000000 &&
        Math.abs(value * 100 - Math.round(value * 100)) < 0.000001;
}

function parseMapJson(value: unknown): WarehouseMap {
    if (!isObject(value) || !Array.isArray(value.nodes) || !Array.isArray(value.edges)) {
        throw new Error("JSON must contain nodes and edges arrays.");
    }
    if (value.nodes.length < 3 || value.nodes.length > 1000) {
        throw new Error("Map must contain between 3 and 1000 nodes.");
    }
    if (value.edges.length < 1 || value.edges.length > 5000) {
        throw new Error("Map must contain between 1 and 5000 edges.");
    }

    const nodes: WarehouseMapNode[] = value.nodes.map((item: unknown, i: number) => {
        if (!isObject(item) || typeof item.code !== "string" ||
            !/^[A-Za-z0-9_-]{1,50}$/.test(item.code)) {
            throw new Error(`Invalid node code at position ${i + 1}.`);
        }
        if (typeof item.type !== "string" ||
            !nodeTypes.includes(item.type as WarehouseNodeType)) {
            throw new Error(`Invalid node type: ${item.code}.`);
        }
        if (!validDecimal(item.x) || !validDecimal(item.y)) {
            throw new Error(`Invalid coordinates: ${item.code}.`);
        }
        return {
            code: item.code,
            type: item.type as WarehouseNodeType,
            x: item.x,
            y: item.y,
        };
    });

    const codes = new Set(nodes.map((node) => node.code));
    if (codes.size !== nodes.length) {
        throw new Error("The map contains duplicate node codes.");
    }
    if (nodes.filter((node) => node.type === "ENTRANCE").length !== 1) {
        throw new Error("The map must have exactly one ENTRANCE.");
    }
    if (nodes.filter((node) => node.type === "EXIT").length !== 1) {
        throw new Error("The map must have exactly one EXIT.");
    }
    if (!nodes.some((node) => node.type === "STORAGE")) {
        throw new Error("The map must contain at least one STORAGE node.");
    }

    const edgeKeys = new Set<string>();
    const edges: WarehouseMapEdge[] = value.edges.map((item: unknown, i: number) => {
        if (!isObject(item) || typeof item.from !== "string" ||
            typeof item.to !== "string" ||
            !codes.has(item.from) || !codes.has(item.to)) {
            throw new Error(`Edge ${i + 1} references an unknown node.`);
        }
        if (item.from === item.to) {
            throw new Error("An edge cannot connect a node to itself.");
        }
        if (!validDecimal(item.distanceM) || item.distanceM <= 0) {
            throw new Error(`Invalid distance for edge ${i + 1}.`);
        }
        if (typeof item.bidirectional !== "boolean") {
            throw new Error(`Invalid direction for edge ${i + 1}.`);
        }
        const key = `${item.from}\u0000${item.to}`;
        if (edgeKeys.has(key)) {
            throw new Error(`Duplicate edge: ${item.from} → ${item.to}.`);
        }
        edgeKeys.add(key);
        return {
            from: item.from,
            to: item.to,
            distanceM: item.distanceM,
            bidirectional: item.bidirectional,
        };
    });

    return { nodes, edges };
}

export default function WarehouseMapsPage({ credentials }: Props) {
    const [warehouses, setWarehouses] = useState<Warehouse[]>([]);
    const [warehouseId, setWarehouseId] = useState("");
    const [savedMap, setSavedMap] = useState<WarehouseMap | null>(null);
    const [previewMap, setPreviewMap] = useState<WarehouseMap | null>(null);
    const [fileName, setFileName] = useState("");
    const [loading, setLoading] = useState(false);
    const [importing, setImporting] = useState(false);
    const [error, setError] = useState("");
    const [success, setSuccess] = useState("");
    const fileInputRef = useRef<HTMLInputElement>(null);

    const hasSavedMap = Boolean(savedMap && savedMap.nodes.length > 0);
    const displayedMap = previewMap ?? savedMap;

    useEffect(() => {
        let active = true;
        getWarehouses(credentials)
            .then((data) => { if (active) setWarehouses(data); })
            .catch((cause: unknown) => {
                if (active) setError(
                    cause instanceof Error ? cause.message : "Cannot load warehouses."
                );
            });
        return () => { active = false; };
    }, [credentials]);

    useEffect(() => {
        if (!warehouseId) return;
        let active = true;
        setLoading(true);
        setError("");
        getWarehouseMap(Number(warehouseId), credentials)
            .then((map) => { if (active) setSavedMap(map); })
            .catch((cause: unknown) => {
                if (active) setError(
                    cause instanceof Error ? cause.message : "Cannot load warehouse map."
                );
            })
            .finally(() => { if (active) setLoading(false); });
        return () => { active = false; };
    }, [warehouseId, credentials]);

    function changeWarehouse(id: string) {
        if (importing) return;
        setWarehouseId(id);
        setSavedMap(null);
        setPreviewMap(null);
        setFileName("");
        setError("");
        setSuccess("");
    }

    async function handleFileChange(event: ChangeEvent<HTMLInputElement>) {
        const file = event.target.files?.[0];
        event.target.value = "";
        if (!file || !warehouseId || loading || importing || hasSavedMap) return;

        setImporting(true);
        setError("");
        setSuccess("");
        setFileName(file.name);
        setPreviewMap(null);

        try {
            if (!file.name.toLowerCase().endsWith(".json")) {
                throw new Error("Please select a .json file.");
            }
            if (file.size > 2 * 1024 * 1024) {
                throw new Error("JSON file must be smaller than 2 MB.");
            }
            let value: unknown;
            try {
                value = JSON.parse(await file.text());
            } catch {
                throw new Error("The selected file does not contain valid JSON.");
            }
            const map = parseMapJson(value);
            setPreviewMap(map);
            const imported = await importWarehouseMap(Number(warehouseId), map, credentials);
            setSavedMap(imported);
            setPreviewMap(null);
            setSuccess(`Map from "${file.name}" imported successfully.`);
        } catch (cause) {
            setPreviewMap(null);
            setError(cause instanceof Error ? cause.message : "Map import failed.");
        } finally {
            setImporting(false);
        }
    }

    function exportMap() {
        if (!savedMap || !warehouseId || !hasSavedMap) return;
        const blob = new Blob([JSON.stringify(savedMap, null, 2)], {
            type: "application/json",
        });
        const url = URL.createObjectURL(blob);
        const link = document.createElement("a");
        link.href = url;
        link.download = `warehouse-${warehouseId}-map.json`;
        document.body.appendChild(link);
        link.click();
        link.remove();
        URL.revokeObjectURL(url);
    }

    return (
        <div className="warehouse-maps-page">
            <div className="page-heading">
                <span className="eyebrow">WAREHOUSE LAYOUT</span>
                <h1>Warehouse Maps</h1>
                <p>Import, inspect and export warehouse layouts stored in PostgreSQL.</p>
            </div>

            <section className="warehouse-map-panel">
                <div className="warehouse-map-header">
                    <div>
                        <span className="eyebrow">SELECT WAREHOUSE</span>
                        <h2>Warehouse layout</h2>
                    </div>
                    {hasSavedMap && (
                        <span className="warehouse-map-saved">Saved in database</span>
                    )}
                </div>
                <label className="warehouse-map-label" htmlFor="warehouse-map-select">
                    Warehouse
                </label>
                <select
                    id="warehouse-map-select"
                    className="warehouse-map-select"
                    value={warehouseId}
                    onChange={(event) => changeWarehouse(event.target.value)}
                    disabled={importing}
                >
                    <option value="">Select a warehouse</option>
                    {warehouses.map((warehouse) => (
                        <option key={warehouse.id} value={warehouse.id}>
                            #{warehouse.id} — {warehouse.name}
                        </option>
                    ))}
                </select>
            </section>

            {error && <div className="warehouse-map-alert error" role="alert">{error}</div>}
            {success && <div className="warehouse-map-alert success" role="status">{success}</div>}

            {warehouseId && (
                <div className="warehouse-map-layout">
                    <section className="warehouse-map-panel">
                        <div className="warehouse-map-header">
                            <div>
                                <span className="eyebrow">GRAPH VISUALIZATION</span>
                                <h2>{previewMap ? "Importing JSON preview" : "Saved warehouse map"}</h2>
                            </div>
                        </div>
                        {loading ? (
                            <div className="warehouse-map-empty" role="status">Loading map...</div>
                        ) : displayedMap && displayedMap.nodes.length > 0 ? (
                            <>
                                <div className="warehouse-map-stats">
                                    <span>{displayedMap.nodes.length} nodes</span>
                                    <span>{displayedMap.edges.length} edges</span>
                                </div>
                                <InteractiveWarehouseMap map={displayedMap} />
                                <div className="warehouse-map-legend">
                                    <span>● Entrance</span>
                                    <span>● Exit</span>
                                    <span>● Storage</span>
                                    <span>● Intersection</span>
                                </div>
                            </>
                        ) : (
                            <div className="warehouse-map-empty">
                                This warehouse has no map yet. Click Import JSON Map to choose a file.
                            </div>
                        )}
                    </section>

                    <section className="warehouse-map-panel">
                        <span className="eyebrow">JSON OPERATIONS</span>
                        <h2>Import / Export</h2>
                        <p className="warehouse-map-help">
                            Choose a JSON file from any folder on your computer.
                            It will be validated and saved automatically.
                        </p>
                        <input
                            ref={fileInputRef}
                            type="file"
                            accept=".json,application/json"
                            onChange={handleFileChange}
                            disabled={loading || importing || hasSavedMap}
                            hidden
                        />
                        <button
                            className="primary-button warehouse-map-button"
                            type="button"
                            disabled={!warehouseId || loading || importing || hasSavedMap}
                            onClick={() => fileInputRef.current?.click()}
                        >
                            {importing ? "Importing map..." : "Import JSON Map"}
                        </button>
                        {fileName && <p className="warehouse-map-filename">Selected: {fileName}</p>}
                        {hasSavedMap && (
                            <p className="warehouse-map-warning">
                                This warehouse already has a map. Import is disabled to protect existing data.
                            </p>
                        )}
                        <button
                            className="warehouse-map-secondary"
                            type="button"
                            onClick={exportMap}
                            disabled={!hasSavedMap || importing}
                        >
                            Export saved JSON
                        </button>
                        <p className="warehouse-map-help">
                            The Spring Boot Backend validates graph connectivity.
                            Existing maps cannot be overwritten.
                        </p>
                    </section>
                </div>
            )}
        </div>
    );
}
