import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it } from "vitest";

import InteractiveWarehouseMap from "../components/InteractiveWarehouseMap";
import type { WarehouseMap } from "../types/warehouseMap";

const testMap: WarehouseMap = {
    nodes: [
        { code: "ENT", type: "ENTRANCE", x: 0, y: 0 },
        { code: "A1", type: "STORAGE", x: 10, y: 0 },
        { code: "EXT", type: "EXIT", x: 20, y: 0 },
    ],
    edges: [
        {
            from: "ENT",
            to: "A1",
            distanceM: 10,
            bidirectional: true,
        },
        {
            from: "A1",
            to: "EXT",
            distanceM: 10,
            bidirectional: true,
        },
    ],
};

describe("InteractiveWarehouseMap", () => {

    it("renders the warehouse graph", () => {
        render(<InteractiveWarehouseMap map={testMap} />);

        expect(
            screen.getByRole("img", {
                name: "Interactive warehouse map",
            })
        ).toBeTruthy();

        expect(screen.getByText("100%")).toBeTruthy();
    });

    it("zooms in when the plus button is clicked", () => {
        render(<InteractiveWarehouseMap map={testMap} />);

        const zoomIndicator = screen.getByText("100%");

        fireEvent.click(screen.getByTitle("Zoom in"));

        const currentZoom = Number(
            zoomIndicator.textContent?.replace("%", "")
        );

        expect(currentZoom).toBeGreaterThan(100);
    });

    it("finds a storage node and displays its details", () => {
        render(<InteractiveWarehouseMap map={testMap} />);

        fireEvent.change(
            screen.getByRole("textbox", {
                name: "Find warehouse node",
            }),
            {
                target: {
                    value: "A1",
                },
            }
        );

        fireEvent.click(
            screen.getByRole("button", {
                name: "Find",
            })
        );

        expect(screen.getByText("Node A1")).toBeTruthy();

        expect(
            document.body.textContent
        ).toContain("STORAGE");
    });

});
