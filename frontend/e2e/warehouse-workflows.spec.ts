
import { test, expect } from "@playwright/test";
import type { Page } from "@playwright/test";

type Role = "MANAGER" | "PICKER";

function credentials(role: Role) {
    const username = process.env[`E2E_${role}_USERNAME`];
    const password = process.env[`E2E_${role}_PASSWORD`];

    if (!username || !password) {
        throw new Error(`Missing E2E_${role} credentials`);
    }

    return { username, password };
}

async function login(page: Page, role: Role) {
    const account = credentials(role);

    await page.goto("/");

    const form = page.locator("form").first();

    await form
        .locator('input:not([type="password"])')
        .first()
        .fill(account.username);

    await form
        .locator('input[type="password"]')
        .fill(account.password);

    await form.locator("button").last().click();

    const navigation = page.getByRole("navigation", {
        name: "Main navigation",
    });

    await expect(
        navigation,
        `Login failed for ${role}. Check credentials and Backend.`
    ).toBeVisible({ timeout: 15000 });

    return navigation;
}

test("manager can access tasks and calculate a route", async ({
                                                                  page,
                                                              }) => {
    const navigation = await login(page, "MANAGER");

    // Verify that the authenticated account is a manager.
    await expect(
        navigation.getByRole("button", {
            name: /User Management/i,
        })
    ).toBeVisible();

    // Open picking tasks.
    const tasksButton = navigation.getByRole("button", {
        name: /Picking Tasks/i,
    });

    await tasksButton.click();
    await expect(tasksButton).toHaveClass(/active/);

    // Open route planner.
    await navigation
        .getByRole("button", {
            name: /Route Planner/i,
        })
        .click();

    await expect(
        page.getByRole("heading", {
            name: /Route Planner/i,
        })
    ).toBeVisible();

    // Select an existing active picking order.
    const firstOrder = page.locator(".rp-order").first();

    await expect(firstOrder).toBeVisible();
    await firstOrder.click();

    // Calculate and verify the route.
    await page
        .getByRole("button", {
            name: /Calculate .*route/i,
        })
        .click();

    await expect(
        page.getByRole("heading", {
            name: /Route comparison/i,
        })
    ).toBeVisible();
});

test("picker can access assigned tasks without manager controls", async ({
                                                                             page,
                                                                         }) => {
    const navigation = await login(page, "PICKER");

    // Manager-only controls must not be visible.
    await expect(
        navigation.getByRole("button", {
            name: /User Management/i,
        })
    ).toHaveCount(0);

    await expect(
        navigation.getByRole("button", {
            name: /Warehouses/i,
        })
    ).toHaveCount(0);

    // The picker opens their own tasks.
    const myTasksButton = navigation.getByRole("button", {
        name: /My Picking Tasks/i,
    });

    await expect(myTasksButton).toBeVisible();
    await myTasksButton.click();

    await expect(myTasksButton).toHaveClass(/active/);

    await expect(
        page.getByText("Access denied.")
    ).toHaveCount(0);
});
