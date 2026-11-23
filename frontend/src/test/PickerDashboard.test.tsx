
import { render, screen } from "@testing-library/react";
import { describe, expect, it, vi } from "vitest";

import PickerDashboard from "../pages/PickerDashboard";

describe("PickerDashboard", () => {

    it("displays the picker workspace", () => {
        render(
            <PickerDashboard
                username="Jordan"
                onRoutes={vi.fn()}
            />
        );

        expect(
            screen.getByRole("heading", {
                name: /picker dashboard/i,
            })
        ).toBeTruthy();

        expect(
            document.body.textContent
        ).toContain("Jordan");
    });

    it("displays the current authenticated username", () => {
        const { rerender } = render(
            <PickerDashboard
                username="Jordan"
                onRoutes={vi.fn()}
            />
        );

        expect(
            document.body.textContent
        ).toContain("Jordan");

        rerender(
            <PickerDashboard
                username="picker_test"
                onRoutes={vi.fn()}
            />
        );

        expect(
            document.body.textContent
        ).toContain("picker_test");
    });

});
