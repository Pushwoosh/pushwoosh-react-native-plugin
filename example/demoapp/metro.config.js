const { getDefaultConfig } = require('@react-native/metro-config');
const path = require('path');

const projectRoot = __dirname;
const packagePath = path.resolve(projectRoot, '../../');

const config = getDefaultConfig(projectRoot);

config.resolver = {
  ...config.resolver,
  nodeModulesPaths: [path.resolve(projectRoot, 'node_modules')],
  extraNodeModules: {
    '@babel/runtime': path.resolve(projectRoot, 'node_modules/@babel/runtime'),
  },
};

config.watchFolders = [packagePath];

module.exports = config;

