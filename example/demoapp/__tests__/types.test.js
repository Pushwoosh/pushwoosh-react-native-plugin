/**
 * @format
 */

import fs from 'fs';
import path from 'path';

const ROOT = path.resolve(__dirname, '../../..');

const wrapperMethods = () => {
  const source = fs.readFileSync(path.join(ROOT, 'index.js'), 'utf8');
  const classBody = source.slice(source.indexOf('class PushNotification {'));
  return new Set(Array.from(classBody.matchAll(/^(?:\t| {4})([a-zA-Z]+)\(/gm), m => m[1]));
};

const declaredMethods = () => {
  const source = fs.readFileSync(path.join(ROOT, 'index.d.ts'), 'utf8');
  const iface = source.slice(source.indexOf('interface Pushwoosh {'));
  return new Set(Array.from(iface.matchAll(/^\s+([a-zA-Z]+)\(/gm), m => m[1]));
};

describe('TypeScript definitions', () => {
  it('declare every public method of the JS wrapper and nothing else', () => {
    const wrapper = [...wrapperMethods()].sort();
    const declared = [...declaredMethods()].sort();
    expect(wrapper.length).toBeGreaterThan(40);
    expect(declared).toEqual(wrapper);
  });

  it('declare the RichMediaStyle constants', () => {
    const source = fs.readFileSync(path.join(ROOT, 'index.d.ts'), 'utf8');
    expect(source).toMatch(/RichMediaStyle:\s*\{\s*MODAL:\s*number;\s*LEGACY:\s*number;?\s*\}/);
  });
});
