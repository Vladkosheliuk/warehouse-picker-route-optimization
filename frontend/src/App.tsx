import { useState } from "react";

import LoginPage from "./pages/LoginPage";
import ManagerDashboard from "./pages/ManagerDashboard";
import PickerDashboard from "./pages/PickerDashboard";
import WarehousesPage from "./pages/WarehousesPage";
import UsersPage from "./pages/UsersPage";
import WarehouseMapsPage from "./pages/WarehouseMapsPage";
import PickingTasksPage from "./pages/PickingTasksPage";
import RoutePlannerPage from "./pages/RoutePlannerPage";

import type { Credentials } from "./api/client";
import type { CurrentUser } from "./api/auth";

import "./App.css";

type Page =
    | "dashboard"
    | "planner"
    | "warehouses"
    | "maps"
    | "users"
    | "tasks"
    | "about";

function AboutAlgorithms() {
  return (
      <>
        <div className="page-heading">
          <span className="eyebrow">METHODOLOGY</span>
          <h1>Routing algorithms</h1>
          <p>
            Compare a weight-aware greedy heuristic against an aisle-based
            S-shape baseline.
          </p>
        </div>
        <div className="about-grid">
          <article className="info-panel">
            <span className="algorithm-tag">01</span>
            <h2>Optimized route</h2>
            <p>
              Uses shortest paths in the warehouse graph and a weight-aware
              greedy rule to choose the next picking location.
            </p>
            <p>
              This is a heuristic. It does not guarantee a globally optimal route.
            </p>
          </article>
          <article className="info-panel">
            <span className="algorithm-tag">02</span>
            <h2>S-shape baseline</h2>
            <p>
              Groups picking locations by aisle coordinates and alternates
              traversal direction between aisles.
            </p>
            <p>
              Its results provide a comparison baseline for the optimized heuristic.
            </p>
          </article>
        </div>
      </>
  );
}

export default function App() {
  const [page, setPage] = useState<Page>("dashboard");
  const [credentials, setCredentials] = useState<Credentials | null>(null);
  const [currentUser, setCurrentUser] = useState<CurrentUser | null>(null);

  function logout() {
    setCredentials(null);
    setCurrentUser(null);
    setPage("dashboard");
  }

  if (!credentials || !currentUser) {
    return (
        <LoginPage
            onAuthenticated={(loginCredentials, user) => {
              setCredentials(loginCredentials);
              setCurrentUser(user);
              setPage("dashboard");
            }}
        />
    );
  }

  const isManager = currentUser.role === "MANAGER";

  const titles: Record<Page, string> = {
    dashboard: isManager ? "Manager Dashboard" : "Picker Dashboard",
    planner: "Route Planner",
    warehouses: "Warehouses",
    maps: "Warehouse Maps",
    users: "User Management",
    tasks: isManager ? "Picking Tasks" : "My Picking Tasks",
    about: "Algorithms",
  };
  const pageTitle = titles[page];

  return (
      <div className="app-shell">
        <aside className="sidebar">
          <div className="brand">
            <div className="brand-mark">W</div>
            <div>
              <strong>PickerRoute</strong>
              <span>Warehouse intelligence</span>
            </div>
          </div>

          <div className="sidebar-label">WORKSPACE</div>
          <nav className="side-nav" aria-label="Main navigation">
            <button
                type="button"
                className={`nav-button ${page === "dashboard" ? "active" : ""}`}
                onClick={() => setPage("dashboard")}
            >
              <span className="nav-symbol">⌂</span>Dashboard
            </button>

            <button
                type="button"
                className={`nav-button ${page === "planner" ? "active" : ""}`}
                onClick={() => setPage("planner")}
            >
              <span className="nav-symbol">▦</span>Route Planner
            </button>

            <button
                type="button"
                className={`nav-button ${page === "tasks" ? "active" : ""}`}
                onClick={() => setPage("tasks")}
            >
              <span className="nav-symbol">☷</span>
              {isManager ? "Picking Tasks" : "My Picking Tasks"}
            </button>

            {isManager && (
                <>
                  <button
                      type="button"
                      className={`nav-button ${page === "warehouses" ? "active" : ""}`}
                      onClick={() => setPage("warehouses")}
                  >
                    <span className="nav-symbol">▤</span>Warehouses
                  </button>
                  <button
                      type="button"
                      className={`nav-button ${page === "maps" ? "active" : ""}`}
                      onClick={() => setPage("maps")}
                  >
                    <span className="nav-symbol">▦</span>
                    Warehouse Maps
                  </button>

                  <button
                      type="button"
                      className={`nav-button ${page === "users" ? "active" : ""}`}
                      onClick={() => setPage("users")}
                  >
                    <span className="nav-symbol">♙</span>User Management
                  </button>
                </>
            )}

            <button
                type="button"
                className={`nav-button ${page === "about" ? "active" : ""}`}
                onClick={() => setPage("about")}
            >
              <span className="nav-symbol">ⓘ</span>About algorithms
            </button>
          </nav>

          <div className="sidebar-bottom">
            <div className="sidebar-info">
              <span className="status-dot" /> Spring Boot API
            </div>
            <small>Java 21 · React · PostgreSQL</small>
            <small>Academic project · 2026</small>
          </div>
        </aside>

        <div className="main-shell">
          <header className="topbar">
            <div className="breadcrumb">
              Workspace <span>/</span> <strong>{pageTitle}</strong>
            </div>
            <div className="topbar-right">
              <span className="environment-label">LOCAL ENVIRONMENT</span>
              <button className="logout-button" onClick={logout} type="button">
                Disconnect
              </button>
            </div>
          </header>

          <main className="main-content">
            {page === "dashboard" ? (
                isManager ? (

                    <ManagerDashboard
                        username={currentUser.username}
                        onWarehouses={() => setPage("warehouses")}
                        onUsers={() => setPage("users")}
                        onRoutes={() => setPage("planner")}
                        onMaps={() => setPage("maps")}
                        {...{ onTasks: () => setPage("tasks") }}
                    />

                ) : (
                    <PickerDashboard
                        username={currentUser.username}
                        onRoutes={() => setPage("planner")}
                        {...{ onTasks: () => setPage("tasks") }}
                    />
                )

            ) : page === "tasks" ? (
                <PickingTasksPage
                    credentials={credentials}
                    role={isManager ? "MANAGER" : "PICKER"}
                />
            ) : page === "maps" && isManager ? (
                <WarehouseMapsPage credentials={credentials} />
            ) : page === "warehouses" && isManager ? (
                <WarehousesPage credentials={credentials} />
            ) : page === "users" && isManager ? (
                <UsersPage credentials={credentials} />
            ) : page === "planner" ? (
                <RoutePlannerPage
                    credentials={credentials}
                    role={isManager ? "MANAGER" : "PICKER"}
                    onUnauthorized={logout}
                />
            ) : page === "about" ? (
                <AboutAlgorithms />
            ) : (
                <div className="error-message" role="alert">
                  Access denied.
                </div>
            )}
          </main>

          <footer className="footer">
            Warehouse Picker Route Optimization Platform
            <span>Bachelor's project · 2026</span>
          </footer>
        </div>
      </div>
  );
}
