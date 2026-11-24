import { useState } from "react";
import type { FormEvent } from "react";
import { getCurrentUser } from "../api/auth";
import type { CurrentUser } from "../api/auth";
import type { Credentials } from "../api/client";
import "./LoginPage.css";

interface LoginPageProps {
    onAuthenticated: (
        credentials: Credentials,
        user: CurrentUser
    ) => void;
}

export default function LoginPage({
                                      onAuthenticated,
                                  }: LoginPageProps) {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");

    async function handleLogin(event: FormEvent<HTMLFormElement>) {
        event.preventDefault();

        const credentials: Credentials = {
            username: username.trim(),
            password,
        };

        if (!credentials.username || !credentials.password) {
            setError("Please enter your username and password.");
            return;
        }

        setLoading(true);
        setError("");

        try {
            const user = await getCurrentUser(credentials);
            onAuthenticated(credentials, user);
        } catch (err) {
            setError(
                err instanceof Error
                    ? err.message
                    : "Unable to sign in."
            );
        } finally {
            setLoading(false);
        }
    }

    return (
        <main className="login-page">
            <div className="login-container">
                <section className="login-intro">
                    <div className="login-logo">
                        <span>W</span>
                        <strong>PickerRoute</strong>
                    </div>

                    <div className="login-intro-content">
            <span className="login-eyebrow">
              WAREHOUSE MANAGEMENT PLATFORM
            </span>

                        <h1>
                            Optimize every route.
                            <br />
                            Simplify every pick.
                        </h1>

                        <p>
                            A centralized workspace for warehouse managers
                            and order pickers.
                        </p>

                        <div className="login-features">
                            <div>Warehouse management</div>
                            <div>Intelligent route planning</div>
                            <div>Picking operations</div>
                        </div>
                    </div>

                    <small>
                        Warehouse Picker Route Optimization Platform · 2026
                    </small>
                </section>

                <section className="login-form-section">
                    <div className="login-form-container">
                        <span className="login-eyebrow">WELCOME BACK</span>

                        <h2>Sign in to your account</h2>

                        <p>
                            Enter the credentials provided by your
                            warehouse administrator.
                        </p>

                        <form onSubmit={handleLogin}>
                            <label htmlFor="login-username">
                                Username
                            </label>

                            <input
                                id="login-username"
                                type="text"
                                value={username}
                                autoComplete="username"
                                onChange={(event) =>
                                    setUsername(event.target.value)
                                }
                                placeholder="Enter your username"
                                required
                            />

                            <label htmlFor="login-password">
                                Password
                            </label>

                            <input
                                id="login-password"
                                type="password"
                                value={password}
                                autoComplete="current-password"
                                onChange={(event) =>
                                    setPassword(event.target.value)
                                }
                                placeholder="Enter your password"
                                required
                            />

                            {error && (
                                <div className="login-error" role="alert">
                                    {error}
                                </div>
                            )}

                            <button type="submit" disabled={loading}>
                                {loading ? "Signing in..." : "Sign in →"}
                            </button>
                        </form>

                        <div className="login-note">
                            Your access level is automatically determined
                            by your account permissions.
                        </div>
                    </div>
                </section>
            </div>
        </main>
    );
}
