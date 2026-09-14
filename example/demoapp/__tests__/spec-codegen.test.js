/**
 * @format
 */

import path from 'path';
import {combineSchemas} from '@react-native/codegen/lib/cli/combine/combine-js-to-schema';
import * as javaSpec from '@react-native/codegen/lib/generators/modules/GenerateModuleJavaSpec';
import * as objcSpec from '@react-native/codegen/lib/generators/modules/GenerateModuleObjCpp';

const SPEC_PATH = path.resolve(__dirname, '../../../src/specs/NativePushwoosh.ts');
const LIBRARY_NAME = 'RNPushwooshSpec';
const JAVA_PACKAGE = 'com.pushwoosh.reactnativeplugin';

// Codegen emits a C++ method per spec method; a method named after a C++ keyword does not compile
// (`register` was the one that bit).
const CPP_KEYWORDS = [
  'alignas', 'alignof', 'and', 'asm', 'auto', 'bitand', 'bitor', 'bool', 'break', 'case', 'catch',
  'char', 'class', 'compl', 'concept', 'const', 'consteval', 'constexpr', 'constinit', 'continue',
  'decltype', 'default', 'delete', 'do', 'double', 'else', 'enum', 'explicit', 'export', 'extern',
  'false', 'float', 'for', 'friend', 'goto', 'if', 'inline', 'int', 'long', 'mutable', 'namespace',
  'new', 'noexcept', 'not', 'nullptr', 'operator', 'or', 'private', 'protected', 'public',
  'register', 'requires', 'return', 'short', 'signed', 'sizeof', 'static', 'struct', 'switch',
  'template', 'this', 'throw', 'true', 'try', 'typedef', 'typeid', 'typename', 'union', 'unsigned',
  'using', 'virtual', 'void', 'volatile', 'while', 'xor',
];

const loadSchema = () => combineSchemas([SPEC_PATH], LIBRARY_NAME);
const moduleOf = schema => schema.modules.NativePushwoosh;

describe('TurboModule spec through codegen', () => {
  it('parses as one native module named after the Objective-C class', () => {
    const module = moduleOf(loadSchema());
    expect(module.type).toBe('NativeModule');
    expect(module.moduleName).toBe('PushwooshPlugin');
    expect(module.spec.eventEmitters).toHaveLength(0);
  });

  it('declares every method with a name codegen can emit as C++', () => {
    const names = moduleOf(loadSchema()).spec.methods.map(m => m.name);
    expect(names.length).toBeGreaterThan(40);
    expect(new Set(names).size).toBe(names.length);
    names.forEach(name => expect(CPP_KEYWORDS).not.toContain(name));
    names.forEach(name => expect(name).toMatch(/^[a-z][A-Za-z0-9]*$/));
  });

  it('generates the Android spec with one abstract method per spec method', () => {
    const schema = loadSchema();
    const files = javaSpec.generate(LIBRARY_NAME, schema, JAVA_PACKAGE);
    const java = files.get(`java/${JAVA_PACKAGE.replace(/\./g, '/')}/NativePushwooshSpec.java`);
    expect(java).toBeDefined();
    expect(java).toContain('public static final String NAME = "PushwooshPlugin";');
    const abstractMethods = java.match(/public abstract void \w+\(/g) || [];
    expect(abstractMethods).toHaveLength(moduleOf(schema).spec.methods.length);
  });

  it('generates the iOS protocol with one method per spec method', () => {
    const schema = loadSchema();
    const files = objcSpec.generate(LIBRARY_NAME, schema, undefined, false);
    const header = files.get('RNPushwooshSpec.h');
    expect(header).toBeDefined();
    expect(header).toContain('@protocol NativePushwooshSpec <RCTBridgeModule, RCTTurboModule>');
    const protocol = header.slice(header.indexOf('@protocol NativePushwooshSpec'));
    const methods = protocol.slice(0, protocol.indexOf('@end')).match(/^- \(void\)/gm) || [];
    expect(methods).toHaveLength(moduleOf(schema).spec.methods.length);
  });
});
