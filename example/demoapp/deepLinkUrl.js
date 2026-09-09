/**
 * @format
 */

// Parsed by hand rather than via URL/URLSearchParams: React Native's polyfill does not
// expose query parameters, so the built-in URL would return an empty params object here.
export function parseDeepLink(url) {
    const match = /^([^:]+):\/\/([^/?#]*)([^?#]*)(?:\?([^#]*))?/.exec(url);
    if (!match) {
        return null;
    }

    const [, scheme, host, path, query] = match;
    const params = {};

    if (query) {
        for (const pair of query.split('&')) {
            if (!pair) {
                continue;
            }
            const [key, value = ''] = pair.split('=');
            params[decodeURIComponent(key)] = decodeURIComponent(value);
        }
    }

    return { scheme, host, path, params };
}
