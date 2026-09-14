/**
 * @format
 */

import React from 'react';
import ReactTestRenderer from 'react-test-renderer';
import App from '../App';

// The tab bar animates on mount; without fake timers the animation outlives the test.
jest.useFakeTimers();

// Mounting the navigator takes tens of seconds on a loaded CI runner - React Navigation 6 on
// React Native 0.74 is the slow end of it - and Jest's default is five. The test is about the
// tree mounting and unmounting at all, not about how fast it does so.
const MOUNT_TIMEOUT_MS = 30000;

test('renders correctly', async () => {
  let tree: ReactTestRenderer.ReactTestRenderer | undefined;

  await ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(<App />);
  });

  await ReactTestRenderer.act(() => {
    tree?.unmount();
  });
}, MOUNT_TIMEOUT_MS);
