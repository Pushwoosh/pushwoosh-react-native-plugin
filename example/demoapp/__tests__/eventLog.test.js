/**
 * @format
 */

import { pushText, recordEvent, subscribeToEvents } from '../eventLog';

test('a subscriber gets the events recorded so far and every new one, newest first', () => {
    const seen = [];
    recordEvent('pushReceived', { title: 'first' });

    const unsubscribe = subscribeToEvents(entries => seen.push(entries.map(e => e.name + ':' + pushText(e.payload))));
    recordEvent('pushOpened', { title: 'second' });
    unsubscribe();
    recordEvent('pushOpened', { title: 'third' });

    expect(seen).toEqual([
        ['pushReceived:first'],
        ['pushOpened:second', 'pushReceived:first'],
    ]);
});

test('pushText reads the message from an Android payload and from both iOS alert shapes', () => {
    expect(pushText({ title: 'Android body', header: 'Android title' })).toBe('Android body');
    expect(pushText({ aps: { alert: 'iOS body' }, pw_msg: '1' })).toBe('iOS body');
    expect(pushText({ aps: { alert: { title: 'iOS title', body: 'iOS body' } } })).toBe('iOS body');
    expect(pushText({ p: 'hash' })).toBe('{"p":"hash"}');
});

test('the log keeps only the newest entries', () => {
    const lengths = [];
    const unsubscribe = subscribeToEvents(entries => lengths.push(entries.length));
    for (let i = 0; i < 30; i++) {
        recordEvent('pushReceived', { title: `push ${i}` });
    }
    unsubscribe();

    expect(Math.max(...lengths)).toBe(20);

    let newest;
    subscribeToEvents(entries => {
        newest = pushText(entries[0].payload);
    })();
    expect(newest).toBe('push 29');
});
