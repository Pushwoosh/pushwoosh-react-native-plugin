module.exports = {
  preset: 'react-native',
  // The plugin is linked from ../../ and has no node_modules of its own: its `react-native`
  // import has to resolve from the demo's node_modules.
  moduleDirectories: ['node_modules', '<rootDir>/node_modules'],
  transformIgnorePatterns: [
    'node_modules/(?!((jest-)?react-native|@react-native(-community)?|@react-navigation|react-native-screens|react-native-safe-area-context|react-native-vector-icons|pushwoosh-react-native-plugin)/)',
  ],
};
