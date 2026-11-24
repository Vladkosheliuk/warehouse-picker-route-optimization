export interface Warehouse {
    id: number;
    name: string;
    description: string | null;
}

export interface WarehouseInput {
    name: string;
    description: string;
}
