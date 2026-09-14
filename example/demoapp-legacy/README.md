# Pushwoosh React Native demo

The sample app for `pushwoosh-react-native-plugin` on React Native 0.74 with the legacy bridge
(`newArchEnabled=false`, plain `pod install`). The same screens run on React Native 0.87 with the
New Architecture in `../demoapp`. To try the New Architecture here: `RCT_NEW_ARCH_ENABLED=1 pod
install` on iOS, `newArchEnabled=true` in `android/gradle.properties` on Android.

Screens: **Actions** (user id, tags, emails, events, HWID, push token, Inbox), **Settings**
(registration, badges, Rich Media style, communication toggle) and **Deep Link** (shows the URL a
push or `pwdemo://` link opened the app with).

## Configure

- Put your Pushwoosh application code into `index.js` (`Pushwoosh.init({ pw_appid: ... })`).
- Android: drop your Firebase `google-services.json` into `android/app/`; replace the
  `com.pushwoosh.apitoken` placeholder in `android/app/src/main/AndroidManifest.xml`.
- iOS: replace the `Pushwoosh_API_TOKEN` placeholder in `ios/demoapp/Info.plist`; set your team in
  Xcode for the `demoapp` and `NotificationService` targets.

## Run

```bash
npm install
npm start                     # Metro

cd ios && pod install && cd ..
npm run ios

npm run android
```

`npm test` runs the Jest suite: the screens render, and the deep link parser and the push event
log behave. The wrapper, spec and typings contract tests live in `../demoapp`.

Every button, text field and switch carries a `testID`, and the Actions screen shows what the last
call returned plus the `pushReceived` / `pushOpened` events the plugin delivered. The end-to-end
flows in `../e2e` drive the app through those ids with [Maestro](https://maestro.mobile.dev).
