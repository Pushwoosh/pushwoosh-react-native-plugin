const {getDefaultConfig, mergeConfig} = require('@react-native/metro-config');
const path = require('path');

// The plugin is linked from ../../ (the SDK sources): Metro has to watch that folder and resolve
// the plugin's own imports from this app's node_modules. The sibling sample and native build
// output under the same root are excluded, or the file watcher runs out of descriptors.
const projectRoot = __dirname;
const packagePath = path.resolve(projectRoot, '../../');
const escape = p => p.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
const blockDir = dir => new RegExp(escape(dir) + '/.*');

const defaultConfig = getDefaultConfig(projectRoot);

const config = {
  resolver: {
    nodeModulesPaths: [path.resolve(projectRoot, 'node_modules')],
    extraNodeModules: {
      '@babel/runtime': path.resolve(projectRoot, 'node_modules/@babel/runtime'),
    },
    blockList: [
  ...(Array.isArray(defaultConfig.resolver.blockList)
    ? defaultConfig.resolver.blockList
    : [defaultConfig.resolver.blockList]),
  blockDir(path.resolve(packagePath, 'example/demoapp-legacy')),
  blockDir(path.resolve(packagePath, 'src/android/build')),
  blockDir(path.resolve(projectRoot, 'ios/build')),
  blockDir(path.resolve(projectRoot, 'ios/Pods')),
  blockDir(path.resolve(projectRoot, 'android/build')),
  blockDir(path.resolve(projectRoot, 'android/app/build')),
].filter(Boolean),
  },
  watchFolders: [packagePath],
};

module.exports = mergeConfig(defaultConfig, config);
