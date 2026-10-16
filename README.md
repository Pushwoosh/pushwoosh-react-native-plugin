<h1 align="center">Pushwoosh React Native Plugin</h1>

<p align="center">
  <a href="https://github.com/Pushwoosh/pushwoosh-react-native-plugin/releases"><img src="https://img.shields.io/github/release/Pushwoosh/pushwoosh-react-native-plugin.svg?style=flat-square" alt="GitHub release"></a>
  <a href="https://www.npmjs.com/package/pushwoosh-react-native-plugin"><img src="https://img.shields.io/npm/v/pushwoosh-react-native-plugin.svg?style=flat-square" alt="npm"></a>
  <a href="https://www.npmjs.com/package/pushwoosh-react-native-plugin"><img src="https://img.shields.io/npm/l/pushwoosh-react-native-plugin.svg?style=flat-square" alt="license"></a>
</p>

<p align="center">
  Cross-platform push notifications, In-App messaging, and more for React Native applications.
</p>

## Table of Contents

- [Documentation](#documentation)
- [Features](#features)
- [Installation](#installation)
- [AI-Assisted Integration](#ai-assisted-integration)
- [Quick Start](#quick-start)
- [API Reference](#api-reference)
- [Support](#support)
- [License](#license)

## Documentation

- [Integration Guide](https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/cross-platform-frameworks/react-native/integrating-react-native-plugin) — step-by-step setup
- [API Reference](docs/README.md) — full API documentation
- [Sample Project](https://github.com/Pushwoosh/pushwoosh-react-native-plugin/tree/master/example/demoapp) — ready-to-run demo app on the current React Native (New Architecture)
- [Legacy Sample Project](https://github.com/Pushwoosh/pushwoosh-react-native-plugin/tree/master/example/demoapp-legacy) — the same demo on React Native 0.74 with the legacy architecture

## Features

- **Push Notifications** — register, receive, and handle push notifications on iOS and Android
- **In-App Messages** — trigger and display in-app messages based on events
- **Tags & Segmentation** — set and get user tags for targeted messaging
- **User Identification** — associate devices with user IDs for cross-device tracking
- **Message Inbox** — built-in UI for message inbox with customization options
- **Badge Management** — set, get, and increment app icon badge numbers
- **Local Notifications** — schedule and manage local notifications
- **Rich Media** — modal and legacy Rich Media presentation styles
- **Huawei Push** — HMS push notification support
- **Multi-channel** — email, SMS, and WhatsApp registration
- **TypeScript Support** — full TypeScript definitions included

## Installation

```bash
npm install pushwoosh-react-native-plugin --save
```

### iOS Setup

```bash
cd ios && pod install
```

That is the whole iOS setup for the app target. The native Pushwoosh SDK comes from the CocoaPods trunk pods
`PushwooshXCFramework` and `PushwooshInboxUIXCFramework`, the same way as in 7.0.x, so upgrading the
plugin needs nothing but `pod install` either.

**CocoaPods trunk becomes read-only on December 2, 2026.** From then on the native SDK gets no
updates through pods: the plugin keeps building on the last version published there, and new native
releases come only as the Swift package
[Pushwoosh-XCFramework](https://github.com/Pushwoosh/Pushwoosh-XCFramework). On React Native 0.75
or newer `pod install` prints a warning about it until the app moves to the package, see
[Moving to the Swift package](#moving-to-the-swift-package). React Native 0.74 cannot take Swift
packages through CocoaPods and stays on the trunk pods.

#### Moving to the Swift package

Recommended on React Native 0.75 or newer (the sample and CI run 0.87), before December 2026. With
the package the app gets the native SDK version each plugin release pins, also after trunk freezes.
If you do not move, nothing breaks: the app keeps building on the trunk pods, but its native SDK
stays at the last version published to trunk.

1. Load the plugin's Podfile helper at the top of `ios/Podfile` and call it after
   `react_native_post_install`:

   ```ruby
   # Resolve the Pushwoosh Podfile helper with node as well, so a hoisted node_modules works too
   require Pod::Executable.execute_command('node', ['-p',
     'require.resolve(
       "pushwoosh-react-native-plugin/scripts/pushwoosh-spm.rb",
       {paths: [process.argv[1]]},
     )', __dir__]).strip

   post_install do |installer|
     react_native_post_install(installer, config[:reactNativePath], :mac_catalyst_enabled => false)
     pushwoosh_spm_post_install(installer)
   end
   ```

   The `require` has to stay at the top level of the Podfile, before the targets: the plugin
   switches to the package when `pod install` reads its podspec, and it checks for the helper at
   that moment. A `require` inside `post_install` comes too late, and the app stays on the trunk
   pods. With the plugin in a plain `<app>/node_modules` the shorter
   `require_relative '../node_modules/pushwoosh-react-native-plugin/scripts/pushwoosh-spm'` does the
   same job.
2. If the project has a Notification Service Extension, pass its target name:
   `pushwoosh_spm_post_install(installer, extra_targets: ['NotificationService'])`. The same goes for
   any other target that references the SDK's classes directly, such as a unit-test bundle that
   mocks them.
3. Remove every `pod 'PushwooshXCFramework'` and `pod 'PushwooshInboxUIXCFramework'` line from the
   Podfile, in all targets. A trunk pod next to the package puts a second copy of the native SDK
   into the app: `pod install` warns about it, and the build fails with
   `Multiple commands produce .../PushwooshFramework.framework`.
4. Run `pod install` and build. The December warning is gone, and `Podfile.lock` no longer lists
   `PushwooshXCFramework`.

The helper adds the package, at the version the plugin pins, and its `PushwooshFramework`,
`PushwooshCore` and `PushwooshBridge` products to every app target that links the plugin. Running
`pod install` again changes nothing, and on React Native below 0.75 the helper does nothing at all.
Both static (the default) and dynamic (`USE_FRAMEWORKS=dynamic`) linking work.

**A notice you can ignore.** With the helper, `pod install` prints React Native's own `Pod
pushwoosh-react-native-plugin is using swift package(s) ... with static linking, this might cause
linker errors`: it is expected, the helper is exactly what makes static linking work.

**First build.** Resolving the package needs network access; without it Xcode stops with a package
resolution error. On a cold package cache the first resolve can fail with `already exists in file
system` while two projects fetch the package's binaries at once: run the build (or `pod install`)
again.

**Expo.** `expo prebuild` regenerates `ios/Podfile`, and its output does not show the `pod install`
warning. Without changes a managed Expo project builds on the trunk pods. To move it to the package,
turn on `ios.useSwiftPackage` of the Pushwoosh Expo config plugin (`pushwoosh-expo-plugin` newer than
2.0.7): prebuild then writes the helper lines into the generated Podfile, see
[iOS: Swift package](https://github.com/Pushwoosh/pushwoosh-expo-plugin#ios-swift-package). A project
that keeps the generated `ios/` directory checked in can edit its Podfile as above instead.

**Message Inbox UI.** On the package, `presentInboxUI()` runs on a framework that ships inside the
plugin: the trunk 7.0.42 build of the Inbox UI without its Mac Catalyst slice, which npm cannot pack
(it is a symlink) and the plugin does not build for anyway. On the trunk pods it comes from
`PushwooshInboxUIXCFramework` 7.0.42, as before.

#### Notification Service Extension

Add a Notification Service Extension to track message delivery and show media attachments on iOS: follow [the native guide](https://docs.pushwoosh.com/developer/pushwoosh-sdk/ios-sdk/setting-up-pushwoosh-ios-sdk/basic-integration-guide/#4-message-delivery-tracking).

The extension runs as a separate process and does not see the application code passed to `Pushwoosh.init()`. Give it the code in one of two ways:

- **`Pushwoosh_APPID` in the app's `Info.plist`.** The extension inherits it from the app, there is no need to repeat it in the extension's `Info.plist`. With plugin versions older than 6.1.59 (iOS SDK before 7.1.0) add it to the extension's `Info.plist` as well.
- **An App Group shared by the app and the extension.** Add the same App Group to both targets (**Signing & Capabilities > App Groups**) and set `PW_APP_GROUPS_NAME` to its identifier in both `Info.plist` files. The extension then reads the application code the app stored there. The App Group is also needed to keep the badge in sync, and for the extension to reach a reverse proxy set with `setReverseProxy()`.

Without the application code the extension sends no delivery event, and with older iOS SDK versions every notification is shown only after about 30 seconds, when the extension times out.

On the trunk pods the extension takes the SDK through a `pod 'PushwooshXCFramework'` line in its
Podfile target. After [moving to the Swift package](#moving-to-the-swift-package) it takes the same
package instead: pass its target name to the helper,
`pushwoosh_spm_post_install(installer, extra_targets: ['NotificationService'])`, or add the package
to that target in Xcode. Its version has to match the plugin's, otherwise the package resolution
fails with a version conflict.

### Android Setup (Firebase Cloud Messaging)

Push notifications on Android require Firebase Cloud Messaging (FCM).

**Option A — Android Studio (recommended):**
Open the `android/` folder in Android Studio, then go to
Tools > Firebase > Cloud Messaging > Set up Firebase Cloud Messaging.
The wizard will automatically add all required configuration.

**Option B — Manual setup:**
1. Create a project in [Firebase Console](https://console.firebase.google.com)
2. Download `google-services.json` and place it in `android/app/`
3. Add the Google Services classpath to `android/build.gradle`:
   ```groovy
   classpath("com.google.gms:google-services:4.3.15")
   ```
4. Apply the plugin in `android/app/build.gradle`:
   ```groovy
   apply plugin: "com.google.gms.google-services"
   ```

The Pushwoosh plugin already includes the `firebase-messaging` dependency, so you do not need to add it manually.

### React Native versions and the New Architecture

The plugin is a Turbo Native Module and also works on the legacy bridge. One package covers both:

| React Native | Architecture | How the plugin runs |
|---|---|---|
| 0.74 – 0.81, `newArchEnabled=false` (default) | Legacy bridge | Bridge module |
| 0.74 – 0.81, `newArchEnabled=true` / `RCT_NEW_ARCH_ENABLED=1` | New Architecture | Codegen TurboModule |
| 0.82 and later | New Architecture only (bridgeless) | Codegen TurboModule |

No interop layer is involved on the New Architecture. Events keep their API on both architectures:

```javascript
import { DeviceEventEmitter } from 'react-native';

DeviceEventEmitter.addListener('pushOpened', (push) => { /* ... */ });
DeviceEventEmitter.addListener('pushReceived', (push) => { /* ... */ });
```

`presentInboxUI()` presents a native screen (a view controller on iOS, an Activity on Android) outside the React view tree, so it does not depend on Fabric. React Native below 0.74 is supported by plugin versions 6.1.x.

Both setups ship as samples with identical screens: `example/demoapp` (React Native 0.87, New Architecture) and `example/demoapp-legacy` (React Native 0.74, legacy architecture).

## AI-Assisted Integration

Integrate the Pushwoosh React Native plugin using AI coding assistants (Claude Code, Cursor, GitHub Copilot, etc.).

> **Requirement:** Your AI assistant must have access to [Context7](https://context7.com/) MCP server or web search capabilities.

### Quick Start Prompts

Choose the prompt that matches your task:

---

#### 1. Basic Plugin Integration

```
Integrate Pushwoosh React Native plugin into my React Native project.

Requirements:
- Install pushwoosh-react-native-plugin via npm
- Initialize Pushwoosh with my App ID in the app entry point
- Register for push notifications and handle pushOpened events via DeviceEventEmitter

Use Context7 MCP to fetch Pushwoosh React Native plugin documentation.
```

---

#### 2. Tags and User Segmentation

```
Show me how to use Pushwoosh tags in a React Native app for user segmentation.
I need to set tags, get tags, and set user ID for cross-device tracking.

Use Context7 MCP to fetch Pushwoosh React Native plugin documentation for setTags and getTags.
```

---

#### 3. Message Inbox Integration

```
Integrate Pushwoosh Message Inbox into my React Native app. Show me how to:
- Display the inbox UI with custom styling
- Load messages programmatically
- Track unread message count

Use Context7 MCP to fetch Pushwoosh React Native plugin documentation for presentInboxUI.
```

---

## Quick Start

### 1. Initialize the Plugin

```javascript
import Pushwoosh from 'pushwoosh-react-native-plugin';
import { DeviceEventEmitter } from 'react-native';

// Listen for push notification events
DeviceEventEmitter.addListener('pushOpened', (e) => {
    console.log("Push opened: " + JSON.stringify(e));
});

// Initialize Pushwoosh
Pushwoosh.init({
    pw_appid: "YOUR_PUSHWOOSH_APP_ID"
});

// Register for push notifications
Pushwoosh.register(
    (token) => {
        console.log("Registered with push token: " + token);
    },
    (error) => {
        console.error("Failed to register: " + error);
    }
);
```

### 2. Set User Tags

```javascript
import Pushwoosh from 'pushwoosh-react-native-plugin';

Pushwoosh.setTags(
    { username: "john_doe", age: 25, interests: ["sports", "tech"] },
    () => console.log("Tags set successfully"),
    (error) => console.error("Failed to set tags: " + error)
);

Pushwoosh.getTags(
    (tags) => console.log("Tags: " + JSON.stringify(tags)),
    (error) => console.error("Get tags error: " + error)
);
```

### 3. Post Events for In-App Messages

```javascript
import Pushwoosh from 'pushwoosh-react-native-plugin';

Pushwoosh.setUserId("user_12345");
Pushwoosh.postEvent("purchase_complete", {
    productName: "Premium Plan",
    amount: "9.99"
});
```

### 4. Message Inbox

```javascript
import Pushwoosh from 'pushwoosh-react-native-plugin';
import { processColor, Image } from 'react-native';

// Open inbox UI with custom styling
Pushwoosh.presentInboxUI({
    dateFormat: "dd.MM.yyyy",
    accentColor: processColor('#3498db'),
    backgroundColor: processColor('#ffffff'),
    titleColor: processColor('#333333'),
    descriptionColor: processColor('#666666'),
    listEmptyMessage: "No messages yet"
});

// Or load messages programmatically
Pushwoosh.loadMessages(
    (messages) => {
        messages.forEach((msg) => {
            console.log(msg.title + ": " + msg.message);
        });
    },
    (error) => console.error("Failed to load: " + error)
);

Pushwoosh.unreadMessagesCount((count) => {
    console.log("Unread messages: " + count);
});
```

### 5. Multi-channel Communication

```javascript
import Pushwoosh from 'pushwoosh-react-native-plugin';

Pushwoosh.setEmails(
    ["user@example.com"],
    () => console.log("Email set"),
    (error) => console.error(error)
);

Pushwoosh.registerSMSNumber("+1234567890");
Pushwoosh.registerWhatsappNumber("+1234567890");
```

### 6. Custom Notification Handling (iOS)

```javascript
import Pushwoosh from 'pushwoosh-react-native-plugin';

Pushwoosh.init({
    pw_appid: "YOUR_PUSHWOOSH_APP_ID",
    pw_notification_handling: "CUSTOM"
});
```

## API Reference

### Initialization & Registration

| Method | Description |
|--------|-------------|
| `init(config, success?, fail?)` | Initialize the plugin. Call on every app launch |
| `register(success?, fail?)` | Register for push notifications |
| `unregister(success?, fail?)` | Unregister from push notifications |
| `getPushToken(success)` | Get the push token |
| `getHwid(success)` | Get Pushwoosh Hardware ID |
| `getUserId(success)` | Get current user ID |

### Tags & User Data

| Method | Description |
|--------|-------------|
| `setTags(tags, success?, fail?)` | Set device tags |
| `getTags(success, fail?)` | Get device tags |
| `setUserId(userId, success?, fail?)` | Set user identifier for cross-device tracking |
| `setLanguage(language)` | Set custom language for localized pushes |
| `setEmails(emails, success?, fail?)` | Register emails for the user |
| `setUserEmails(userId, emails, success?, fail?)` | Set user ID and register emails |
| `registerSMSNumber(phoneNumber)` | Register SMS number (E.164 format) |
| `registerWhatsappNumber(phoneNumber)` | Register WhatsApp number (E.164 format) |

### Notifications

| Method | Description |
|--------|-------------|
| `createLocalNotification(config)` | Schedule a local notification |
| `clearLocalNotification()` | Clear all pending local notifications |
| `clearNotificationCenter()` | Clear all notifications from notification center |

### Badge Management

| Method | Description |
|--------|-------------|
| `setApplicationIconBadgeNumber(badge)` | Set badge number |
| `getApplicationIconBadgeNumber(callback)` | Get current badge number |
| `addToApplicationIconBadgeNumber(badge)` | Increment/decrement badge |

### In-App Messages & Events

| Method | Description |
|--------|-------------|
| `postEvent(event, attributes?)` | Post event to trigger In-App Messages |
| `setRichMediaType(type)` | Set Rich Media style (MODAL or LEGACY) |
| `getRichMediaType(callback)` | Get current Rich Media style |

### Message Inbox

| Method | Description |
|--------|-------------|
| `presentInboxUI(style?)` | Open inbox UI with optional style customization |
| `loadMessages(success, fail?)` | Load inbox messages programmatically |
| `unreadMessagesCount(callback)` | Get unread message count |
| `messagesCount(callback)` | Get total message count |
| `messagesWithNoActionPerformedCount(callback)` | Get messages with no action count |
| `readMessage(id)` | Mark message as read |
| `readMessages(ids)` | Mark multiple messages as read |
| `deleteMessage(id)` | Delete a message |
| `deleteMessages(ids)` | Delete multiple messages |
| `performAction(id)` | Perform the action associated with a message |

### Android-specific

| Method | Description |
|--------|-------------|
| `setMultiNotificationMode(on)` | Allow multiple notifications in notification center |
| `setLightScreenOnNotification(on)` | Turn screen on when notification arrives |
| `setEnableLED(on)` | Enable LED blinking on notification |
| `setColorLED(color)` | Set LED color (ARGB integer) |
| `setSoundType(type)` | Set sound type (0=default, 1=none, 2=always) |
| `setVibrateType(type)` | Set vibration type (0=default, 1=none, 2=always) |
| `setNotificationIconBackgroundColor(color)` | Set notification icon background color |
| `enableHuaweiPushNotifications()` | Enable Huawei HMS push support |

### Communication Control

| Method | Description |
|--------|-------------|
| `setCommunicationEnabled(enable, success?, fail?)` | Enable/disable all Pushwoosh communication |
| `isCommunicationEnabled(success)` | Check if communication is enabled |

### Reverse Proxy

| Method | Description |
|--------|-------------|
| `setReverseProxy(url, headers?)` | Route all SDK requests through a reverse proxy. Call before `init()` |

### Events (DeviceEventEmitter)

| Event | Description |
|-------|-------------|
| `pushOpened` | Fired when a notification is opened by the user |
| `pushReceived` | Fired when a notification is received |

## Support

- [Documentation](https://docs.pushwoosh.com/)
- [Support Portal](https://support.pushwoosh.com/)
- [Report Issues](https://github.com/Pushwoosh/pushwoosh-react-native-plugin/issues)

## License

Pushwoosh React Native Plugin is available under the MIT license. See [LICENSE](LICENSE) for details.

---

Made with ❤️ by [Pushwoosh](https://www.pushwoosh.com/)
