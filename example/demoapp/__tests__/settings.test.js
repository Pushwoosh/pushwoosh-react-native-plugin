/**
 * @format
 */

import React from 'react';
import ReactTestRenderer from 'react-test-renderer';

// The screen under test is the real one; only the native boundary is faked. TurboModuleRegistry
// is swapped the same way __tests__/plugin.test.js swaps it, so the plugin's JS facade - including
// its optional-callback handling - runs for real between the screen and the fake.
jest.mock('react-native/Libraries/TurboModule/TurboModuleRegistry', () => {
  const actual = jest.requireActual(
    'react-native/Libraries/TurboModule/TurboModuleRegistry',
  );
  return {
    ...actual,
    get: name =>
      name === 'PushwooshPlugin'
        ? global.__pushwooshNativeModule
        : actual.get(name),
  };
});

// A native module that answers nothing by default: each test fills in the calls it is about.
// `token` decides whether the screen starts with registration on.
const makeNative = ({token = '', ...overrides} = {}) => ({
  getPushToken: jest.fn(callback => callback(token)),
  getRichMediaType: jest.fn(callback => callback(1)),
  setCommunicationEnabled: jest.fn(),
  registerForPushNotifications: jest.fn(),
  unregister: jest.fn(),
  ...overrides,
});

const renderSettings = async native => {
  global.__pushwooshNativeModule = native;
  let Settings;
  jest.isolateModules(() => {
    // Pin 'react' to the outer singleton: isolateModules would otherwise hand Settings a second
    // React instance, whose hook dispatcher the renderer below never sets.
    jest.doMock('react', () => React);
    Settings = require('../Settings').default;
  });

  let tree;
  await ReactTestRenderer.act(() => {
    tree = ReactTestRenderer.create(<Settings />);
  });
  return tree;
};

// findAllByProps and index 0, not findByProps: a testID reaches both the composite element and
// the host node it renders, and findByProps throws when it matches more than one.
const byTestID = (tree, id) => tree.root.findAllByProps({testID: id})[0];

const resultText = tree => byTestID(tree, 'settings-result').props.children;

const registerSwitch = tree => byTestID(tree, 'register-switch');

const toggle = async (tree, value) => {
  await ReactTestRenderer.act(() => {
    registerSwitch(tree).props.onValueChange(value);
  });
};

afterEach(() => {
  delete global.__pushwooshNativeModule;
  jest.restoreAllMocks();
});

describe('turning registration off', () => {
  it('reports success and leaves the switch off once native confirms', async () => {
    const native = makeNative({
      token: 'device-token',
      unregister: jest.fn(success => success('')),
    });
    const tree = await renderSettings(native);
    expect(registerSwitch(tree).props.value).toBe(true);

    await toggle(tree, false);

    expect(native.unregister).toHaveBeenCalledTimes(1);
    expect(resultText(tree)).toBe('Unregistered from pushes');
    expect(registerSwitch(tree).props.value).toBe(false);
  });

  it('reports the error and keeps the switch on when native fails', async () => {
    const native = makeNative({
      token: 'device-token',
      unregister: jest.fn((success, error) => error('no network')),
    });
    const tree = await renderSettings(native);

    await toggle(tree, false);

    expect(resultText(tree)).toBe('Failed to unregister: no network');
    expect(registerSwitch(tree).props.value).toBe(true);
  });

  it('says nothing until native answers', async () => {
    const native = makeNative({token: 'device-token'});
    const tree = await renderSettings(native);

    await toggle(tree, false);

    expect(native.unregister).toHaveBeenCalledTimes(1);
    expect(resultText(tree)).toBe('');
    expect(registerSwitch(tree).props.value).toBe(true);
  });
});

describe('turning registration on', () => {
  it('reports the token and leaves the switch on once native confirms', async () => {
    const native = makeNative({
      registerForPushNotifications: jest.fn(success => success('new-token')),
    });
    const tree = await renderSettings(native);
    expect(registerSwitch(tree).props.value).toBe(false);

    await toggle(tree, true);

    expect(resultText(tree)).toBe('Registered for pushes: new-token');
    expect(registerSwitch(tree).props.value).toBe(true);
  });

  it('reports the error and keeps the switch off when native fails', async () => {
    const native = makeNative({
      registerForPushNotifications: jest.fn((success, error) =>
        error('permission denied'),
      ),
    });
    const tree = await renderSettings(native);

    await toggle(tree, true);

    expect(resultText(tree)).toBe('Failed to register: permission denied');
    expect(registerSwitch(tree).props.value).toBe(false);
  });
});
