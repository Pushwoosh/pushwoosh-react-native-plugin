# REACT NATIVE SAMPLE 

## To launch and utilize a sample with Pushwoosh SDK integration, clone or download the repository archive.

### iOS
 <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/iOS_1.png" alt="Alt text" width="300"> <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/iOS_2.png" alt="Alt text" width="300"> 

### Android
 <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/Android_1.png" alt="Alt text" width="300"> <img src="https://github.com/Pushwoosh/pushwoosh-reactnative-sample/blob/main/Screenshots/Android_2.png" alt="Alt text" width="300"> 

### 1. Open demoapp -> index.js and set your Pushwoosh App ID.

```js
Pushwoosh.init({ "pw_appid": "XXXXX-XXXXX" });
```

### 2. Set up Firebase Cloud Messaging for Android.

Open the `android/` folder in Android Studio, then go to
Tools > Firebase > Cloud Messaging > Set up Firebase Cloud Messaging.

Or manually:
1. Add `google-services.json` to `android/app/`
2. Add `classpath("com.google.gms:google-services:4.3.15")` to root `build.gradle`
3. Add `apply plugin: "com.google.gms.google-services"` to `app/build.gradle`

## The guide for SDK integration is available on Pushwoosh [website](https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/cross-platform-frameworks/react-native/integrating-react-native-plugin).

Documentation:
https://github.com/Pushwoosh/pushwoosh-ios-sdk/tree/master/Documentation

Pushwoosh team
http://www.pushwoosh.com
