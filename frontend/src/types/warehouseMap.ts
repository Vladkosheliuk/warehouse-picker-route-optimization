
export type WarehouseNodeType =
    | "ENTRANCE"
    | "EXIT"
    | "INTERSECTION"
    | "STORAGE";

export interface WarehouseMapNode {
    code: string;
    type: WarehouseNodeType;
    x: number;
    y: number;
}

export interface WarehouseMapEdge {
    from: string;
    to: string;
    distanceM: number;
    bidirectional: boolean;
}

export interface WarehouseMap {
    nodes: WarehouseMapNode[];
    edges: WarehouseMapEdge[];
}
