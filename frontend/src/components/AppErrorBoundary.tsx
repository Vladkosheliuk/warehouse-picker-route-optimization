import { Component } from "react";
import type { ErrorInfo, ReactNode } from "react";
import "./AppErrorBoundary.css";

type Props = {
    children: ReactNode;
};

type State = {
    hasError: boolean;
};


export default class AppErrorBoundary extends Component<Props, State> {
    state: State = { hasError: false };

    static getDerivedStateFromError(): State {
        return { hasError: true };
    }

    componentDidCatch(error: Error, info: ErrorInfo): void {
        // Do not include credentials, headers, or API response bodies here.
        console.error("Unexpected application rendering error", error.name, info.componentStack);
    }

    render() {
        if (this.state.hasError) {
            return (
                <main className="app-error-boundary" role="alert">
                    <div className="app-error-boundary__card">
                        <h1>Something went wrong</h1>
                        <p>
                            The application could not display this page. You can reload the
                            application and sign in again.
                        </p>
                        <button type="button" onClick={() => window.location.reload()}>
                            Reload application
                        </button>
                    </div>
                </main>
            );
        }

        return this.props.children;
    }
}
