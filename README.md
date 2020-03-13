React Native Pushwoosh Push Notifications module
===================================================

[![GitHub release](https://img.shields.io/github/release/Pushwoosh/pushwoosh-react-native-plugin.svg?style=flat-square)](https://github.com/Pushwoosh/pushwoosh-react-native-plugin/releases) 
[![npm](https://img.shields.io/npm/v/pushwoosh-react-native-plugin.svg)](https://www.npmjs.com/package/pushwoosh-react-native-plugin)
[![license](https://img.shields.io/npm/l/pushwoosh-react-native-plugin.svg)](https://www.npmjs.com/package/pushwoosh-react-native-plugin)

![platforms](https://img.shields.io/badge/platforms-Android%20%7C%20iOS-yellowgreen.svg)

| [Guide](https://www.pushwoosh.com/platform-docs/pushwoosh-sdk/cross-platform-frameworks/react-native/integrating-react-native-plugin) | [Documentation](https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/cross-platform-frameworks/react-native/integrating-react-native-plugin) | [Sample](https://github.com/Pushwoosh/pushwoosh-react-native-sample) |
| ----------------------------------------------------------- | ------------------------------- | -------------------------------------------------------------------- |


### Installation

```
npm install pushwoosh-react-native-plugin --save
react-native link pushwoosh-react-native-plugin
```

### Usage

```js
import Pushwoosh from 'pushwoosh-react-native-plugin';

Pushwoosh.init({ 
    "pw_appid" : "YOUR_PUSHWOOSH_PROJECT_ID" , 
    "project_number" : "YOUR_GCM_PROJECT_NUMBER" 
});
Pushwoosh.register();
```
