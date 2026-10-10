let nextToastId = 1;
let toasts = [];
const listeners = new Set();
const timers = new Map();

export const subscribeToToasts = (listener) => {
    listeners.add(listener);
    return () => listeners.delete(listener);
};

export const getToastsSnapshot = () => toasts;

export function showToast(message, type = "error") {
    if (typeof message !== "string" || !message.trim()) return;

    const id = nextToastId++;
    toasts = [...toasts.slice(-2), {id, message: message.trim(), type}];
    listeners.forEach(listener => listener());

    const timer = setTimeout(() => dismissToast(id), type === "error" ? 7000 : 4500);
    timers.set(id, timer);
}

export function dismissToast(id) {
    const timer = timers.get(id);
    if (timer) clearTimeout(timer);
    timers.delete(id);
    toasts = toasts.filter(toast => toast.id !== id);
    listeners.forEach(listener => listener());
}

export function getApiErrorMessage(error, fallback = "Something went wrong. Please try again.") {
    const response = error?.response?.data;
    if (typeof response === "string" && response.trim()) return response;

    if (typeof response?.message === "string" && response.message.trim()) {
        const details = Object.values(response.errors || {})
            .flatMap(value => Array.isArray(value) ? value : [value])
            .filter(value => typeof value === "string" && value.trim());
        return details.length
            ? `${response.message}: ${details.join(", ")}`
            : response.message;
    }

    return error?.message || fallback;
}
