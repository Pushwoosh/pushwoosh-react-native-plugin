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
    const { params } = parseDeepLink('pwdemo://demo/screen?from=t1&title=%D0%A1%D0%BF%D0%B0%D1%80%D1%82%D0%B0%D0%BA');

    expect(params).toEqual({ from: 't1', title: 'Спартак' });
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
