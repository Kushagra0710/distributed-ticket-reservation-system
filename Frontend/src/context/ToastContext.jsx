import { useSyncExternalStore } from "react";
import { dismissToast, getToastsSnapshot, subscribeToToasts } from "./toastService";

export function ToastProvider({children}) {
    const activeToasts = useSyncExternalStore(subscribeToToasts, getToastsSnapshot, getToastsSnapshot);

    return (
        <>
            {children}
            <div
                aria-live="polite"
                aria-relevant="additions removals"
                className="pointer-events-none fixed right-4 top-4 z-[100] flex w-[min(24rem,calc(100vw-2rem))] flex-col gap-2 sm:right-6 sm:top-6"
            >
                {activeToasts.map(toast => (
                    <div
                        key={toast.id}
                        role={toast.type === "error" ? "alert" : "status"}
                        className={`pointer-events-auto flex items-start gap-3 rounded-lg border px-4 py-3 text-sm shadow-lg ${
                            toast.type === "error"
                                ? "border-red-200 bg-white text-red-900"
                                : toast.type === "success"
                                    ? "border-green-200 bg-white text-green-900"
                                    : "border-blue-200 bg-white text-blue-900"
                        }`}
                    >
                        <p className="min-w-0 flex-1 break-words">{toast.message}</p>
                        <button
                            type="button"
                            aria-label="Dismiss notification"
                            onClick={() => dismissToast(toast.id)}
                            className="shrink-0 rounded px-1 text-lg leading-5 opacity-70 hover:opacity-100 focus:outline-none focus:ring-2 focus:ring-gray-400"
                        >
                            &times;
                        </button>
                    </div>
                ))}
            </div>
        </>
    );
}
