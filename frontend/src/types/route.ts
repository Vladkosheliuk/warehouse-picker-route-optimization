export interface RouteStop {
    nodeId: number;
    productId: number | null;
    quantity: number;
    distanceFromPreviousM: number;
    carriedWeightAfterKg: number;
}

export interface RouteResult {
    stops: RouteStop[];
    totalDistanceM: number;
    loadDistanceKgM: number;
    fullPathNodeIds: number[];
}

export interface RouteComparison {
    taskId: number;
    warehouseId: number;
    optimized: RouteResult;
    sShape: RouteResult;
}
