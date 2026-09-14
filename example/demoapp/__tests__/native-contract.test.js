/**
 * @format
 */

import fs from 'fs';
import path from 'path';
import {combineSchemas} from '@react-native/codegen/lib/cli/combine/combine-js-to-schema';

const ROOT = path.resolve(__dirname, '../../..');
const SPEC_PATH = path.join(ROOT, 'src/specs/NativePushwoosh.ts');
const IOS_MODULE = path.join(ROOT, 'src/ios/PushwooshPlugin/Pushwoosh.mm');
const JAVA_MODULE = path.join(
  ROOT,
  'src/android/src/main/java/com/pushwoosh/reactnativeplugin/PushwooshPlugin.java',
);

// Provided by the RCTEventEmitter base class on iOS, so the module does not export them itself.
const IOS_INHERITED = ['addListener', 'removeListeners'];
// Legacy method kept on both platforms for apps that still call it; not part of the spec on purpose.
const NATIVE_EXTRAS = ['onPushReceived'];

const specMethods = () => combineSchemas([SPEC_PATH], 'RNPushwooshSpec').modules.NativePushwoosh.spec.methods;

// RCT_EXPORT_METHOD(name:(T)arg label:(T)arg ...) -> { name, labels: [label, ...] }
const iosExports = () => {
  const source = fs.readFileSync(IOS_MODULE, 'utf8');
  const exports = [];
  const re = /RCT_EXPORT_METHOD\(([^{]*?)\)\s*\{/g;
  let match;
  while ((match = re.exec(source)) !== null) {
    const selector = match[1].replace(/\([^)]*\)\s*\w+/g, '').replace(/\s+/g, '');
    const parts = selector.split(':').filter(Boolean);
    exports.push({name: parts[0], labels: parts.slice(1)});
  }
  return exports;
};

// @ReactMethod public void name(Type a, Type b) -> { name, paramCount }
const javaExports = () => {
  const source = fs.readFileSync(JAVA_MODULE, 'utf8');
  const exports = [];
  const re = /@ReactMethod\s+public void (\w+)\(([^)]*)\)/g;
  let match;
  while ((match = re.exec(source)) !== null) {
    const params = match[2].trim() === '' ? [] : match[2].split(',');
    exports.push({name: match[1], paramCount: params.length});
  }
  return exports;
};

describe('spec versus native implementations', () => {
  it('names the module after the Objective-C class on both platforms', () => {
    const ios = fs.readFileSync(IOS_MODULE, 'utf8');
    const java = fs.readFileSync(JAVA_MODULE, 'utf8');
    expect(ios).toMatch(/RCT_EXPORT_MODULE\(\);/);
    expect(ios).toMatch(/@implementation PushwooshPlugin\b/);
    expect(java).toContain('public static final String MODULE_NAME = "PushwooshPlugin";');
    expect(combineSchemas([SPEC_PATH], 'RNPushwooshSpec').modules.NativePushwoosh.moduleName).toBe(
      'PushwooshPlugin',
    );
  });

  it('implements every spec method on iOS with the selector labels codegen derives', () => {
    const ios = new Map(iosExports().map(e => [e.name, e]));
    specMethods()
      .filter(m => !IOS_INHERITED.includes(m.name))
      .forEach(method => {
        const exported = ios.get(method.name);
        expect(exported).toBeDefined();
        const labels = method.typeAnnotation.params.slice(1).map(p => p.name);
        expect(exported.labels).toEqual(labels);
      });
  });

  it('implements every spec method on Android with the same arity', () => {
    const java = new Map(javaExports().map(e => [e.name, e]));
    specMethods().forEach(method => {
      const exported = java.get(method.name);
      expect(exported).toBeDefined();
      expect(exported.paramCount).toBe(method.typeAnnotation.params.length);
    });
  });

  it('exports nothing from native that the spec does not know about', () => {
    const spec = new Set(specMethods().map(m => m.name));
    const allowed = new Set([...spec, ...NATIVE_EXTRAS]);
    iosExports().forEach(e => expect(allowed).toContain(e.name));
    javaExports().forEach(e => expect(allowed).toContain(e.name));
  });
});
