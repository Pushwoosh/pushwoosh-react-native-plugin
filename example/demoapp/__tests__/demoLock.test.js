/**
 * @format
 */

import fs from 'fs';
import path from 'path';

// The demo installs the plugin as a local link, so npm copies the plugin's own manifest fields
// into the lock's "../.." entry and rewrites them on every install. Left behind, they turn a
// plain `npm install` into a dirty working tree for whoever builds the demo next.
const LOCK_PATH = path.resolve(__dirname, '../package-lock.json');
const PLUGIN_MANIFEST_PATH = path.resolve(__dirname, '../../../package.json');
const LINK_KEY = '../..';

const lock = JSON.parse(fs.readFileSync(LOCK_PATH, 'utf8'));
const plugin = JSON.parse(fs.readFileSync(PLUGIN_MANIFEST_PATH, 'utf8'));

describe(`the lock entry for ${LINK_KEY}`, () => {
  it('exists', () => {
    expect(Object.keys(lock.packages)).toContain(LINK_KEY);
  });

  it('carries the plugin name and version from Sources/package.json', () => {
    expect(lock.packages[LINK_KEY]).toMatchObject({
      name: plugin.name,
      version: plugin.version,
    });
  });

  it('carries the plugin peerDependencies from Sources/package.json', () => {
    expect(lock.packages[LINK_KEY].peerDependencies).toEqual(
      plugin.peerDependencies,
    );
  });
});
