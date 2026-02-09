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
- [Sample Project](https://github.com/Pushwoosh/pushwoosh-react-native-sample) — ready-to-run demo app

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

### Android Setup

Add to your project's `build.gradle`:

```groovy
buildscript {
    dependencies {
        classpath 'com.google.gms:google-services:4.3.15'
    }
}
```

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
    pw_appid: "YOUR_PUSHWOOSH_APP_ID",
    project_number: "YOUR_FCM_SENDER_ID"
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
    project_number: "YOUR_FCM_SENDER_ID",
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
