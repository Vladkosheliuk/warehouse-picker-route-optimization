interface Props {
    username: string;
    onRoutes: () => void;
}

export default function PickerDashboard({
                                            username,
                                            onRoutes,
                                        }: Props) {
    return (
        <div>
            <div className="page-heading">
                <span className="eyebrow">PICKING WORKSPACE</span>
                <h1>Picker Dashboard</h1>
                <p>
                    Welcome back, {username}. Your workspace for
                    warehouse picking operations.
                </p>
            </div>

            <div className="dashboard-grid">
                <article className="dashboard-card upcoming">
                    <div className="dashboard-card-icon">☷</div>
                    <h2>My Picking Tasks</h2>
                    <p>
                        View orders assigned to your account
                        and select a task to begin picking.
                    </p>
                    <span className="dashboard-upcoming">
            Under development
          </span>
                </article>

                <article className="dashboard-card">
                    <div className="dashboard-card-icon">⌁</div>
                    <h2>Route Planner</h2>
                    <p>
                        Calculate a route for an existing task.
                        Manual task IDs are temporary.
                    </p>
                    <button onClick={onRoutes} type="button">
                        Open route planner →
                    </button>
                </article>

                <article className="dashboard-card upcoming">
                    <div className="dashboard-card-icon">✓</div>
                    <h2>Picking Progress</h2>
                    <p>
                        Mark collected products and track
                        the progress of an assigned task.
                    </p>
                    <span className="dashboard-upcoming">
            Under development
          </span>
                </article>
            </div>
        </div>
    );
}
