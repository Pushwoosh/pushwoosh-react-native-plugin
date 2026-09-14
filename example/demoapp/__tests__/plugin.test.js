/**
 * @format
 */

import fs from 'fs';
import path from 'path';

// The spec module asks TurboModuleRegistry for the native module when it is imported; the
// registry is swapped so each test decides what "native" looks like without a native binary.
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

const SPEC_PATH = path.resolve(
  __dirname,
  '../../../src/specs/NativePushwoosh.ts',
);

// Method names declared in the TurboModule spec: the only methods JS may call on native.
const specMethods = () =>
  Array.from(
    fs.readFileSync(SPEC_PATH, 'utf8').matchAll(/^\s+(\w+)\(/gm),
    m => m[1],
  );

// A native module that answers only to spec methods; anything else is a TypeError, as it would
// be on a real TurboModule.
const makeNativeModule = () => {
  const native = {};
  specMethods().forEach(name => {
    native[name] = jest.fn();
  });
  return native;
};

const loadPlugin = nativeModule => {
  global.__pushwooshNativeModule = nativeModule;
  let plugin;
  jest.isolateModules(() => {
    plugin = require('pushwoosh-react-native-plugin');
  });
  return plugin;
};

const F = expect.any(Function);
const cb = () => {};

// Wrapper methods whose native counterpart has another name (`register` is a C++ keyword).
const nativeName = {register: 'registerForPushNotifications'};

// [wrapper method, wrapper args, native args]
const forwarding = [
  [
    'setReverseProxy',
    ['https://proxy.example.com/', {'X-Auth': 't'}],
    ['https://proxy.example.com/', {'X-Auth': 't'}],
  ],
  [
    'setReverseProxy',
    ['https://proxy.example.com/'],
    ['https://proxy.example.com/', null],
  ],
  ['init', [{pw_appid: 'XXXXX-XXXXX'}], [{pw_appid: 'XXXXX-XXXXX'}, F, F]],
  [
    'init',
    [{pw_appid: 'XXXXX-XXXXX'}, cb, cb],
    [{pw_appid: 'XXXXX-XXXXX'}, cb, cb],
  ],
  [
    'createLocalNotification',
    [{msg: 'hi', seconds: 5}],
    [{msg: 'hi', seconds: 5}],
  ],
  ['clearLocalNotification', [], []],
  ['clearNotificationCenter', [], []],
  ['register', [], [F, F]],
  ['register', [cb, cb], [cb, cb]],
  ['unregister', [], [F, F]],
  ['onPushOpen', [cb], [cb]],
  ['setUserEmails', ['user-1', 'a@b.c'], ['user-1', ['a@b.c'], F, F]],
  [
    'setUserEmails',
    ['user-1', ['a@b.c', 'd@e.f'], cb, cb],
    ['user-1', ['a@b.c', 'd@e.f'], cb, cb],
  ],
  ['setEmails', ['a@b.c'], [['a@b.c'], F, F]],
  ['setEmails', [['a@b.c'], cb, cb], [['a@b.c'], cb, cb]],
  ['registerSMSNumber', ['+1234567890'], ['+1234567890']],
  ['registerWhatsappNumber', ['+1234567890'], ['+1234567890']],
  ['setTags', [{plan: 'pro'}], [{plan: 'pro'}, F, F]],
  ['getTags', [cb], [cb, F]],
  ['setShowPushnotificationAlert', [false], [false]],
  ['getShowPushnotificationAlert', [cb], [cb]],
  ['getPushToken', [cb], [cb]],
  ['getHwid', [cb], [cb]],
  ['getUserId', [cb], [cb]],
  ['setUserId', ['user-1'], ['user-1', F, F]],
  ['setUserId', ['user-1', cb, cb], ['user-1', cb, cb]],
  ['postEvent', ['purchase'], ['purchase', {}]],
  ['postEvent', ['purchase', {amount: '9.99'}], ['purchase', {amount: '9.99'}]],
  ['setApplicationIconBadgeNumber', [5], [5]],
  ['getApplicationIconBadgeNumber', [cb], [cb]],
  ['addToApplicationIconBadgeNumber', [-1], [-1]],
  ['setMultiNotificationMode', [true], [true]],
  ['setLightScreenOnNotification', [true], [true]],
  ['setEnableLED', [true], [true]],
  ['setColorLED', [0xff00ff00], [0xff00ff00]],
  ['setSoundType', [1], [1]],
  ['setVibrateType', [2], [2]],
  [
    'presentInboxUI',
    [{dateFormat: 'dd.MM.yyyy'}],
    [{dateFormat: 'dd.MM.yyyy'}],
  ],
  ['presentInboxUI', [], [undefined]],
  ['messagesWithNoActionPerformedCount', [cb], [cb]],
  ['unreadMessagesCount', [cb], [cb]],
  ['messagesCount', [cb], [cb]],
  ['loadMessages', [cb], [cb, F]],
  ['readMessage', ['code-1'], ['code-1']],
  ['readMessages', [['code-1', 'code-2']], [['code-1', 'code-2']]],
  ['deleteMessage', ['code-1'], ['code-1']],
  ['deleteMessages', [['code-1']], [['code-1']]],
  ['performAction', ['code-1'], ['code-1']],
  ['isCommunicationEnabled', [cb], [cb]],
  ['setCommunicationEnabled', [false], [false, F, F]],
  ['setNotificationIconBackgroundColor', ['#ff0000'], ['#ff0000']],
  ['setLanguage', ['de'], ['de']],
  ['enableHuaweiPushNotifications', [], []],
  ['setRichMediaType', [1], [1]],
  ['getRichMediaType', [cb], [cb]],
];

describe('pushwoosh-react-native-plugin JS wrapper', () => {
  afterEach(() => {
    delete global.__pushwooshNativeModule;
    jest.restoreAllMocks();
  });

  it('declares every public method in the TurboModule spec', () => {
    const plugin = loadPlugin(makeNativeModule());
    const publicMethods = Object.getOwnPropertyNames(
      Object.getPrototypeOf(plugin),
    ).filter(name => name !== 'constructor' && name !== 'RichMediaStyle');
    const spec = specMethods();
    expect(publicMethods.length).toBeGreaterThan(40);
    publicMethods.forEach(name =>
      expect(spec).toContain(nativeName[name] || name),
    );
  });

  it.each(forwarding)(
    '%s(%j) reaches native as %j',
    (method, args, nativeArgs) => {
      const native = makeNativeModule();
      const plugin = loadPlugin(native);

      plugin[method](...args);

      const nativeMethod = native[nativeName[method] || method];
      expect(nativeMethod).toHaveBeenCalledTimes(1);
      expect(nativeMethod).toHaveBeenCalledWith(...nativeArgs);
    },
  );

  it('rejects an empty user id for setUserEmails without calling native', () => {
    const native = makeNativeModule();
    const plugin = loadPlugin(native);
    const fail = jest.fn();

    plugin.setUserEmails('', ['a@b.c'], cb, fail);

    expect(fail).toHaveBeenCalledWith('userId must not be empty');
    expect(native.setUserEmails).not.toHaveBeenCalled();
  });

  it('rejects an empty email list without calling native', () => {
    const native = makeNativeModule();
    const plugin = loadPlugin(native);
    const fail = jest.fn();

    plugin.setEmails([], cb, fail);

    expect(fail).toHaveBeenCalledWith(
      'emails must be a non-empty array of strings',
    );
    expect(native.setEmails).not.toHaveBeenCalled();
  });

  it('warns and skips native for a blank phone number', () => {
    const native = makeNativeModule();
    const plugin = loadPlugin(native);
    const warn = jest.spyOn(console, 'warn').mockImplementation(() => {});

    plugin.registerSMSNumber('   ');
    plugin.registerWhatsappNumber('');

    expect(warn).toHaveBeenCalledTimes(2);
    expect(native.registerSMSNumber).not.toHaveBeenCalled();
    expect(native.registerWhatsappNumber).not.toHaveBeenCalled();
  });

  it('exposes RichMediaStyle constants', () => {
    const plugin = loadPlugin(makeNativeModule());
    expect(plugin.RichMediaStyle).toEqual({MODAL: 0, LEGACY: 1});
  });

  it('imports without a native module and fails only when a method is called', () => {
    const plugin = loadPlugin(null);
    expect(() => plugin.getHwid(cb)).toThrow(TypeError);
  });
});
