/**
 * @format
 */

// The push events the plugin delivers through DeviceEventEmitter, kept for the screen that lists
// them. index.js records them as they arrive, before any screen exists, so the log lives here
// rather than in component state; newest first.
const entries = [];
const listeners = new Set();

// The screen shows the newest few; keeping every payload a long-lived app ever received would
// hold them all for the lifetime of the process and copy a growing array on every push.
const MAX_ENTRIES = 20;

export function recordEvent(name, payload) {
    entries.unshift({ name, payload, at: Date.now() });
    entries.length = Math.min(entries.length, MAX_ENTRIES);
    listeners.forEach(listener => listener([...entries]));
}

// Calls `listener` with the current entries right away and on every new event; returns the
// unsubscribe function.
export function subscribeToEvents(listener) {
    listeners.add(listener);
    listener([...entries]);
    return () => listeners.delete(listener);
}

// The text a push shows: Android sends it as "title", iOS inside aps.alert as a string or an
// object with a body.
export function pushText(payload) {
    if (!payload || typeof payload !== 'object') {
        return String(payload);
    }
    if (typeof payload.title === 'string') {
        return payload.title;
    }
    const alert = payload.aps?.alert;
    if (typeof alert === 'string') {
        return alert;
    }
    if (alert && typeof alert.body === 'string') {
        return alert.body;
    }
    return JSON.stringify(payload);
}
