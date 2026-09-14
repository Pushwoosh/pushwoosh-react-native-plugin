/**
 * @format
 */

import fs from 'fs';
import path from 'path';

// The two samples run the same screens on two React Native versions, so the JS below is a verbatim
// copy in both. The Maestro flows in example/e2e/flows/common are shared and address both through
// the same testIDs, so a change landing in one copy only would leave the shared flows asserting
// against code a single row has - and the matrix would stop testing the same thing on both
// architectures without failing. Editing one copy fails here instead.
const SHARED_FILES = [
    'index.js',
    'App.tsx',
    'Actions.js',
    'Settings.js',
    'DeepLink.js',
    'deepLinkUrl.js',
    'eventLog.js',
    '__tests__/deepLink.test.js',
    '__tests__/eventLog.test.js',
    '__tests__/App.test.tsx',
];

const HERE = path.resolve(__dirname, '..');
const OTHER = path.resolve(HERE, '..', 'demoapp-legacy');

test.each(SHARED_FILES)('%s is the same in both samples', file => {
    const here = fs.readFileSync(path.join(HERE, file), 'utf8');
    const other = fs.readFileSync(path.join(OTHER, file), 'utf8');

    expect(other).toBe(here);
});
