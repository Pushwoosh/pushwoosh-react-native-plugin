/**
 * @format
 */

import { parseDeepLink } from '../deepLinkUrl';

test('splits a deep link into scheme, host, path and query params', () => {
    expect(parseDeepLink('pwdemo://demo/screen?from=t1')).toEqual({
        scheme: 'pwdemo',
        host: 'demo',
        path: '/screen',
        params: { from: 't1' },
    });
});

test('reads every query parameter and decodes escaped values', () => {
    const { params } = parseDeepLink('pwdemo://demo/screen?from=t1&title=H%C3%A9llo%20w%C3%B6rld');

    expect(params).toEqual({ from: 't1', title: 'Héllo wörld' });
});

test('returns an empty params object for a link without a query', () => {
    expect(parseDeepLink('pwdemo://demo/screen')).toEqual({
        scheme: 'pwdemo',
        host: 'demo',
        path: '/screen',
        params: {},
    });
});

test('returns null for a value that is not a link', () => {
    expect(parseDeepLink('just some text')).toBeNull();
});
