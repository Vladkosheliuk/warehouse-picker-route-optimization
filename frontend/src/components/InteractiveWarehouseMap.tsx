
import {
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import type {
    FormEvent,
    PointerEvent as ReactPointerEvent,
} from "react";

import type {
    WarehouseMap,
    WarehouseNodeType,
} from "../types/warehouseMap";

import "./InteractiveWarehouseMap.css";

interface Props {
    map: WarehouseMap;
}

interface Point {
    x: number;
    y: number;
}

interface Viewport {
    x: number;
    y: number;
    width: number;
    height: number;
}

interface PanState {
    pointerId: number;
    startX: number;
    startY: number;
    viewport: Viewport;
    scaleX: number;
    scaleY: number;
}

const WIDTH = 1000;
const HEIGHT = 600;
const PADDING = 40;
const MAX_ZOOM = 32;

const INITIAL_VIEW: Viewport = {
    x: 0,
    y: 0,
    width: WIDTH,
    height: HEIGHT,
};

function clamp(value: number, min: number, max: number) {
    return Math.max(min, Math.min(max, value));
}

function clampViewport(view: Viewport): Viewport {
    return {
        ...view,
        x: clamp(view.x, 0, WIDTH - view.width),
        y: clamp(view.y, 0, HEIGHT - view.height),
    };
}

function zoomAround(
    view: Viewport,
    factor: number,
    focus: Point
): Viewport {
    const currentZoom = WIDTH / view.width;
    const nextZoom = clamp(currentZoom * factor, 1, MAX_ZOOM);

    const nextWidth = WIDTH / nextZoom;
    const nextHeight = HEIGHT / nextZoom;

    const relativeX = (focus.x - view.x) / view.width;
    const relativeY = (focus.y - view.y) / view.height;

    return clampViewport({
        x: focus.x - relativeX * nextWidth,
        y: focus.y - relativeY * nextHeight,
        width: nextWidth,
        height: nextHeight,
    });
}

function screenToSvg(
    svg: SVGSVGElement,
    clientX: number,
    clientY: number
): Point | null {
    const matrix = svg.getScreenCTM();

    if (!matrix) return null;

    const point = new DOMPoint(clientX, clientY);
    const transformed = point.matrixTransform(matrix.inverse());

    return {
        x: transformed.x,
        y: transformed.y,
    };
}

function colorFor(type: WarehouseNodeType): string {
    switch (type) {
        case "ENTRANCE":
            return "#2563eb";
        case "EXIT":
            return "#059669";
        case "STORAGE":
            return "#d97706";
        case "INTERSECTION":
            return "#64748b";
    }
}

export default function InteractiveWarehouseMap({
                                                    map,
                                                }: Props) {
    const svgRef = useRef<SVGSVGElement>(null);
    const panRef = useRef<PanState | null>(null);

    const [viewport, setViewport] =
        useState<Viewport>(INITIAL_VIEW);

    const [dragging, setDragging] = useState(false);
    const [fullscreen, setFullscreen] = useState(false);

    const [selectedCode, setSelectedCode] =
        useState<string | null>(null);

    const [search, setSearch] = useState("");
    const [searchError, setSearchError] = useState("");

    const dense = map.nodes.length > 150;
    const zoom = WIDTH / viewport.width;

    const nodeRadius = dense ? 2.4 : 10;
    const showLabels = !dense || zoom >= 4;
    const showDistances =
        map.edges.length <= 30 || zoom >= 7;

    const positions = useMemo(() => {
        const result = new Map<string, Point>();

        if (map.nodes.length === 0) {
            return result;
        }

        const xs = map.nodes.map((node) => node.x);
        const ys = map.nodes.map((node) => node.y);

        const minX = Math.min(...xs);
        const maxX = Math.max(...xs);
        const minY = Math.min(...ys);
        const maxY = Math.max(...ys);

        const rangeX = maxX - minX;
        const rangeY = maxY - minY;

        const scale = Math.min(
            (WIDTH - PADDING * 2) / Math.max(rangeX, 1),
            (HEIGHT - PADDING * 2) / Math.max(rangeY, 1)
        );

        const offsetX = (WIDTH - rangeX * scale) / 2;
        const offsetY = (HEIGHT - rangeY * scale) / 2;

        for (const node of map.nodes) {
            result.set(node.code, {
                x: offsetX + (node.x - minX) * scale,
                y: offsetY + (node.y - minY) * scale,
            });
        }

        return result;
    }, [map]);

    useEffect(() => {
        setViewport(INITIAL_VIEW);
        setSelectedCode(null);
        setSearch("");
        setSearchError("");
    }, [map]);

    useEffect(() => {
        const svg = svgRef.current;
        if (!svg) return;

        function handleWheel(event: WheelEvent) {
            event.preventDefault();

            if (!svg) return;

            const point = screenToSvg(
                svg,
                event.clientX,
                event.clientY
            );

            if (!point) return;

            setViewport((previous) =>
                zoomAround(
                    previous,
                    event.deltaY < 0 ? 1.25 : 0.8,
                    point
                )
            );
        }

        svg.addEventListener("wheel", handleWheel, {
            passive: false,
        });

        return () => {
            svg.removeEventListener("wheel", handleWheel);
        };
    }, []);

    useEffect(() => {
        if (!fullscreen) return;

        const previousOverflow = document.body.style.overflow;
        document.body.style.overflow = "hidden";

        function handleKeyDown(event: KeyboardEvent) {
            if (event.key === "Escape") {
                setFullscreen(false);
            }
        }

        document.addEventListener("keydown", handleKeyDown);

        return () => {
            document.body.style.overflow = previousOverflow;
            document.removeEventListener(
                "keydown",
                handleKeyDown
            );
        };
    }, [fullscreen]);

    function changeZoom(factor: number) {
        setViewport((previous) =>
            zoomAround(previous, factor, {
                x: previous.x + previous.width / 2,
                y: previous.y + previous.height / 2,
            })
        );
    }

    function resetZoom() {
        setViewport(INITIAL_VIEW);
    }

    function handlePointerDown(
        event: ReactPointerEvent<SVGSVGElement>
    ) {
        if (event.button !== 0) return;

        const svg = event.currentTarget;
        const matrix = svg.getScreenCTM();

        if (!matrix) return;

        panRef.current = {
            pointerId: event.pointerId,
            startX: event.clientX,
            startY: event.clientY,
            viewport,
            scaleX: Math.max(
                Math.hypot(matrix.a, matrix.b),
                0.001
            ),
            scaleY: Math.max(
                Math.hypot(matrix.c, matrix.d),
                0.001
            ),
        };

        svg.setPointerCapture(event.pointerId);
        setDragging(true);
    }

    function handlePointerMove(
        event: ReactPointerEvent<SVGSVGElement>
    ) {
        const pan = panRef.current;

        if (!pan || pan.pointerId !== event.pointerId) {
            return;
        }

        const deltaX = event.clientX - pan.startX;
        const deltaY = event.clientY - pan.startY;

        setViewport(
            clampViewport({
                ...pan.viewport,
                x: pan.viewport.x - deltaX / pan.scaleX,
                y: pan.viewport.y - deltaY / pan.scaleY,
            })
        );
    }

    function handlePointerEnd(
        event: ReactPointerEvent<SVGSVGElement>
    ) {
        if (panRef.current?.pointerId !== event.pointerId) {
            return;
        }

        panRef.current = null;
        setDragging(false);

        if (
            event.currentTarget.hasPointerCapture(event.pointerId)
        ) {
            event.currentTarget.releasePointerCapture(
                event.pointerId
            );
        }
    }

    function handleFindNode(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const node = map.nodes.find(
            (item) =>
                item.code.toLowerCase() ===
                search.trim().toLowerCase()
        );

        if (!node) {
            setSearchError("Node not found.");
            return;
        }

        const point = positions.get(node.code);
        if (!point) return;

        setSelectedCode(node.code);
        setSearchError("");

        const nextZoom = dense ? 10 : 5;
        const width = WIDTH / nextZoom;
        const height = HEIGHT / nextZoom;

        setViewport(
            clampViewport({
                x: point.x - width / 2,
                y: point.y - height / 2,
                width,
                height,
            })
        );
    }

    const visibleNodes = useMemo(() => {
        const margin = 24;

        return map.nodes.filter((node) => {
            const point = positions.get(node.code);
            if (!point) return false;

            return (
                point.x >= viewport.x - margin &&
                point.x <=
                viewport.x + viewport.width + margin &&
                point.y >= viewport.y - margin &&
                point.y <=
                viewport.y + viewport.height + margin
            );
        });
    }, [map, positions, viewport]);

    const visibleEdges = useMemo(() => {
        return map.edges.filter((edge) => {
            const a = positions.get(edge.from);
            const b = positions.get(edge.to);

            if (!a || !b) return false;

            const margin = 15;

            return !(
                Math.max(a.x, b.x) < viewport.x - margin ||
                Math.min(a.x, b.x) >
                viewport.x + viewport.width + margin ||
                Math.max(a.y, b.y) < viewport.y - margin ||
                Math.min(a.y, b.y) >
                viewport.y + viewport.height + margin
            );
        });
    }, [map, positions, viewport]);

    const selectedNode = map.nodes.find(
        (node) => node.code === selectedCode
    );

    const connectedEdges = selectedNode
        ? map.edges.filter(
            (edge) =>
                edge.from === selectedNode.code ||
                edge.to === selectedNode.code
        ).length
        : 0;

    return (
        <section
            className={`map-viewer ${
                fullscreen ? "map-viewer--fullscreen" : ""
            }`}
        >
            <div className="map-viewer__toolbar">
                <div className="map-viewer__controls">
                    <button
                        type="button"
                        onClick={() => changeZoom(1 / 1.5)}
                        disabled={zoom <= 1}
                        title="Zoom out"
                    >
                        −
                    </button>

                    <span className="map-viewer__zoom">
            {Math.round(zoom * 100)}%
          </span>

                    <button
                        type="button"
                        onClick={() => changeZoom(1.5)}
                        disabled={zoom >= MAX_ZOOM}
                        title="Zoom in"
                    >
                        +
                    </button>

                    <button
                        type="button"
                        onClick={resetZoom}
                        title="Fit to screen"
                    >
                        Fit
                    </button>
                </div>

                <form
                    className="map-viewer__search"
                    onSubmit={handleFindNode}
                >
                    <input
                        value={search}
                        onChange={(event) =>
                            setSearch(event.target.value)
                        }
                        placeholder="Find node code"
                        aria-label="Find warehouse node"
                    />

                    <button type="submit">Find</button>
                </form>

                <button
                    type="button"
                    className="map-viewer__fullscreen-button"
                    onClick={() =>
                        setFullscreen((previous) => !previous)
                    }
                >
                    {fullscreen ? "Close fullscreen" : "Fullscreen"}
                </button>
            </div>

            {searchError && (
                <p className="map-viewer__error" role="alert">
                    {searchError}
                </p>
            )}

            <div className="map-viewer__viewport">
                <svg
                    ref={svgRef}
                    viewBox={`${viewport.x} ${viewport.y} ${viewport.width} ${viewport.height}`}
                    className={`map-viewer__svg ${
                        dragging ? "is-dragging" : ""
                    }`}
                    role="img"
                    aria-label="Interactive warehouse map"
                    onPointerDown={handlePointerDown}
                    onPointerMove={handlePointerMove}
                    onPointerUp={handlePointerEnd}
                    onPointerCancel={handlePointerEnd}
                >
                    <defs>
                        <marker
                            id="interactive-map-arrow"
                            markerWidth="6"
                            markerHeight="6"
                            refX="5"
                            refY="3"
                            orient="auto"
                        >
                            <path
                                d="M0 0 L0 6 L6 3 Z"
                                fill="#94a3b8"
                            />
                        </marker>
                    </defs>

                    <rect
                        x="0"
                        y="0"
                        width={WIDTH}
                        height={HEIGHT}
                        fill="#f8fafc"
                    />

                    {visibleEdges.map((edge, index) => {
                        const a = positions.get(edge.from)!;
                        const b = positions.get(edge.to)!;

                        return (
                            <g key={`${edge.from}-${edge.to}-${index}`}>
                                <line
                                    x1={a.x}
                                    y1={a.y}
                                    x2={b.x}
                                    y2={b.y}
                                    stroke="#a3afc0"
                                    strokeWidth={dense ? 1.1 : 2.5}
                                    markerEnd={
                                        !edge.bidirectional && zoom >= 3
                                            ? "url(#interactive-map-arrow)"
                                            : undefined
                                    }
                                />

                                {showDistances && (
                                    <text
                                        x={(a.x + b.x) / 2}
                                        y={(a.y + b.y) / 2 - 6}
                                        fontSize={dense ? 3.5 : 12}
                                        textAnchor="middle"
                                        fill="#475569"
                                        paintOrder="stroke"
                                        stroke="#f8fafc"
                                        strokeWidth={dense ? 1 : 3}
                                    >
                                        {edge.distanceM} m
                                    </text>
                                )}
                            </g>
                        );
                    })}

                    {visibleNodes.map((node) => {
                        const point = positions.get(node.code)!;
                        const selected = selectedCode === node.code;

                        return (
                            <g
                                key={node.code}
                                className="map-viewer__node"
                                onPointerDown={(event) =>
                                    event.stopPropagation()
                                }
                                onClick={() => setSelectedCode(node.code)}
                            >
                                <circle
                                    cx={point.x}
                                    cy={point.y}
                                    r={nodeRadius}
                                    fill={colorFor(node.type)}
                                    stroke={selected ? "#0f172a" : "#ffffff"}
                                    strokeWidth={selected ? 2 : dense ? 0.5 : 2}
                                />

                                {showLabels && (
                                    <text
                                        x={point.x}
                                        y={
                                            point.y +
                                            (dense ? 6 : nodeRadius + 15)
                                        }
                                        fontSize={dense ? 3.5 : 12}
                                        fontWeight="600"
                                        fill="#334155"
                                        textAnchor="middle"
                                    >
                                        {node.code}
                                    </text>
                                )}

                                <title>
                                    {node.code} · {node.type}
                                </title>
                            </g>
                        );
                    })}
                </svg>
            </div>

            <div className="map-viewer__footer">
                {selectedNode ? (
                    <div className="map-viewer__selected">
                        <strong>Node {selectedNode.code}</strong>

                        <span>Type: {selectedNode.type}</span>

                        <span>
              Coordinates: ({selectedNode.x},{" "}
                            {selectedNode.y})
            </span>

                        <span>
              Connected edges: {connectedEdges}
            </span>

                        <button
                            type="button"
                            onClick={() => setSelectedCode(null)}
                        >
                            Clear
                        </button>
                    </div>
                ) : (
                    <p>
                        Scroll to zoom · Drag to move ·
                        Click a node for details
                    </p>
                )}
            </div>
        </section>
    );
}
