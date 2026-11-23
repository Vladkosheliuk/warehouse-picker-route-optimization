
interface Props {
    username: string;
    onWarehouses: () => void;
    onUsers: () => void;
    onRoutes: () => void;
    onMaps: () => void;
}

export default function ManagerDashboard({
                                             username,
                                             onWarehouses,
                                             onUsers,
                                             onRoutes,
                                             onMaps,
                                         }: Props) {
    return (
        <div>
            <div className="page-heading">
                <span className="eyebrow">MANAGEMENT WORKSPACE</span>
                <h1>Manager Dashboard</h1>
                <p>
                    Welcome back, {username}. Manage your warehouse
                    operations from one workspace.
                </p>
            </div>

            <div className="dashboard-grid">
                <article className="dashboard-card">
                    <div className="dashboard-card-icon">▤</div>
                    <h2>Warehouse Management</h2>
                    <p>Create, edit and manage warehouse records.</p>
                    <button onClick={onWarehouses} type="button">
                        Manage warehouses →
                    </button>
                </article>

                <article className="dashboard-card">
                    <div className="dashboard-card-icon">♙</div>
                    <h2>User Management</h2>
                    <p>
                        Create manager and picker accounts,
                        and view warehouse employees.
                    </p>
                    <button onClick={onUsers} type="button">
                        Manage employees →
                    </button>
                </article>

                <article className="dashboard-card">
                    <div className="dashboard-card-icon">⌁</div>
                    <h2>Route Optimization</h2>
                    <p>
                        Calculate routes and compare
                        weight-aware and S-shape algorithms.
                    </p>
                    <button onClick={onRoutes} type="button">
                        Open route planner →
                    </button>
                </article>

                <article className="dashboard-card">
                    <div className="dashboard-card-icon">▦</div>
                    <h2>Warehouse Maps</h2>
                    <p>
                        Import warehouse layouts from JSON,
                        preview nodes and connections,
                        and export stored maps.
                    </p>
                    <button onClick={onMaps} type="button">
                        Open warehouse maps →
                    </button>
                </article>

                <article className="dashboard-card upcoming">
                    <div className="dashboard-card-icon">☷</div>
                    <h2>Picking Tasks</h2>
                    <p>
                        Create picking tasks, select products
                        and assign employees.
                    </p>
                    <span className="dashboard-upcoming">
            Under development
          </span>
                </article>
            </div>
        </div>
    );
}
