# REACT NATIVE SAMPLE 

## To launch and utilize a sample with Pushwoosh SDK integration, clone or download the repository archive.

### iOS
 <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/iOS_1.png" alt="Alt text" width="300"> <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/iOS_2.png" alt="Alt text" width="300"> 

### Android
 <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/Android_1.png" alt="Alt text" width="300"> <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/Android_2.png" alt="Alt text" width="300"> 

### 1. Open demoapp -> Settings.js and add your App ID and FCM Sender ID.

```
/**
* initialize Pushwoosh SDK.
* Example params: {"pw_appid": "application id", "project_number": "FCM sender id"}
* 
* 1. app_id - YOUR_APP_ID
* 2. sender_id - FCM_SENDER_ID
*/

Pushwoosh.init({ "pw_appid" : "XXXXX-XXXXX", "project_number":"XXXXXXXXXXXX"});

```

### 2. Add 'google-services.json' file in android -> app folder.

### 3. Add GoogleServices gradle plugin to your project's build.gradle

```
// you should already have buildscript and dependencies blocks in your project's build.gradle so just put the classpath line there

buildscript {
  dependencies {
  classpath 'com.google.gms:google-services:4.3.3'
  }
}

```

### 4. Apply GoogleServicesPlugin in your app's build.gradle

```
// add these lines to the very end of your build.gradle

apply {
 plugin com.google.gms.googleservices.GoogleServicesPlugin
}

```

## The guide for SDK integration is available on Pushwoosh [website](https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/cross-platform-frameworks/react-native/integrating-react-native-plugin).

Documentation:
https://github.com/Pushwoosh/pushwoosh-ios-sdk/tree/master/Documentation

Pushwoosh team
http://www.pushwoosh.com
