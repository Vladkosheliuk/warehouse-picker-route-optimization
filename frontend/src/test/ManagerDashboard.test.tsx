import { fireEvent, render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import ManagerDashboard from "../pages/ManagerDashboard";

describe("ManagerDashboard", () => {

    it("displays the manager workspace", () => {
        render(
            <ManagerDashboard
                username="test_manager"
                onWarehouses={vi.fn()}
                onUsers={vi.fn()}
                onRoutes={vi.fn()}
                onMaps={vi.fn()}
            />
        );

        expect(
            screen.getByRole("heading", {
                name: /manager dashboard/i,
            })
        ).toBeTruthy();

        expect(
            screen.getByText("Warehouse Management")
        ).toBeTruthy();

        expect(
            screen.getByText("User Management")
        ).toBeTruthy();
    });

    it("opens warehouse management when clicked", () => {
        const onWarehouses = vi.fn();

        render(
            <ManagerDashboard
                username="test_manager"
                onWarehouses={onWarehouses}
                onUsers={vi.fn()}
                onRoutes={vi.fn()}
                onMaps={vi.fn()}
            />
        );

        fireEvent.click(
            screen.getByRole("button", {
                name: /manage warehouses/i,
            })
        );

        expect(onWarehouses).toHaveBeenCalledOnce();
    });

});
