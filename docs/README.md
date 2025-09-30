
# Pushwoosh React Native Module #

Provides module for React Native to receive and handle push notifications for iOS and Android.

Example:

```js
var Pushwoosh = require('pushwoosh-react-native-plugin');

Pushwoosh.init({ "pw_appid" : "PUSHWOOSH_APP_ID" , "project_number" : "GOOGLE_PROJECT_NUMBER" });

Pushwoosh.register(
  (token) => {
    console.warn("Registered for push notifications with token: " + token);
  },
  (error) => {
    console.warn("Failed to register for push notifications: " + error);
  }
);

// this event is fired when the push is received in the app
DeviceEventEmitter.addListener('pushReceived', (e: Event) => {
  console.warn("pushReceived: " + JSON.stringify(e));
});

// this event is fired when user clicks on notification
DeviceEventEmitter.addListener('pushOpened', (e: Event) => {
  console.warn("pushOpened: " + JSON.stringify(e));
});
```

<br>
<h3>Summary</h3>
<hr />
<table width=100% style='background-color:#0EA7ED;'>
<tbody>
<tr>
<th align="left" colspan="2"><strong>Functions</strong></th>
</tr>
<tr class="even"><td><a href="#init">init(config, success, fail)</a></td></tr>
<tr class="even"><td><a href="#register">register(success, fail)</a></td></tr>
<tr class="even"><td><a href="#unregister">unregister(success, fail)</a></td></tr>
<tr class="even"><td><a href="#settags">setTags(tags, success, fail)</a></td></tr>
<tr class="even"><td><a href="#gettags">getTags(success, fail)</a></td></tr>
<tr class="even"><td><a href="#getpushtoken">getPushToken(success)</a></td></tr>
<tr class="even"><td><a href="#gethwid">getHwid(success)</a></td></tr>
<tr class="even"><td><a href="#setuserid">setUserId(userId, success, fail)</a></td></tr>
<tr class="even"><td><a href="#postevent">postEvent(event, attributes)</a></td></tr>
<tr class="even"><td><a href="#setapplicationiconbadgenumber">setApplicationIconBadgeNumber(badgeNumber)</a></td></tr>
<tr class="even"><td><a href="#getapplicationiconbadgenumber">getApplicationIconBadgeNumber(callback)</a></td></tr>
<tr class="even"><td><a href="#addtoapplicationiconbadgenumber">addToApplicationIconBadgeNumber(badgeNumber)</a></td></tr>
<tr class="even"><td><a href="#setmultinotificationmode">setMultiNotificationMode(on)</a></td></tr>
<tr class="even"><td><a href="#setlightscreenonnotification">setLightScreenOnNotification(on)</a></td></tr>
<tr class="even"><td><a href="#setenableled">setEnableLED(on)</a></td></tr>
<tr class="even"><td><a href="#setcolorled">setColorLED(color)</a></td></tr>
<tr class="even"><td><a href="#setsoundtype">setSoundType(type)</a></td></tr>
<tr class="even"><td><a href="#setvibratetype">setVibrateType(type)</a></td></tr>
<tr class="even"><td><a href="#presentinboxui">presentInboxUI()</a></td></tr>
<tr class="even"><td><a href="#setcommunicationenabled">setCommunicationEnabled()</a></td></tr>
<tr class="even"><td><a href="#enableHuaweiPushNotifications">enableHuaweiPushNotifications()</a></td></tr>
<tr class="even"><td><a href="#registersmsnumber">registerSMSNumber(phoneNumber)</a></td></tr>
<tr class="even"><td><a href="#registerwhatsappnumber">registerWhatsappNumber(phoneNumber)</a></td></tr>
<tr>
<th align="left" colspan="2"><strong>Events</strong></th>
</tr>
<tr class="even"><td><a href="#pushopened">pushOpened</a></td></tr>
<tr class="even"><td><a href="#pushreceived">pushReceived</a></td></tr>
</tbody>
</table>
<hr />


### init

```js
init(config, success, fail)
```

Initializes Pushwoosh module with application id and google project number.

<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>object</td><td><b>config</b></td><td>Pushwoosh initialization config.</td></tr>
<tr class="even"><td>string</td><td><b>config.pw_appid</b></td><td>Pushwoosh application id.</td></tr>
<tr class="even"><td>string</td><td><b>config.project_number</b></td><td>GCM project number (for Android push notifications).</td></tr>
<tr class="even"><td>string</td><td><b>config.reverse_proxy_url</b></td><td>(optional) Url to your reverse proxy.</td></tr>
<tr class="even"><td>string</td><td><b>config.pw_notification_handling</b></td><td>(optional) To use custom notification handling on iOS specify the parameter "pw_notification_handling" to "CUSTOM".</td></tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>(optional) initialization success callback.</td></tr>
<tr class="even"><td>function</td><td><b>fail</b></td><td>(optional) initialization failure callback.</td></tr>
</tbody>
</table>

### register

```js
register(success, fail)
```

Registers current device for push notifications.


<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>(optional) registration success callback. Receives push token as parameter.</td></tr>
<tr class="even"><td>function</td><td><b>fail</b></td><td>(optional) registration failure callback.</td></tr>
</tbody>
</table>

NOTE: if user does not allow application to receive push notifications and `UIBackgroundModes remote-notificaion` is not set in **Info.plist** none of these callbacks will be called.


### unregister

```js
unregister(success, fail)
```

Unregisters current deivce from receiving push notifications.


<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>(optional) deregistration success callback.</td></tr>
<tr class="even"><td>function</td><td><b>fail</b></td><td>(optional) deregistration failure callback.</td></tr>
</tbody>
</table>


### setTags

```js
setTags(tags, success, fail)
```

Set tags associated with current device and application.

<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>object</td><td><b>tags</b></td><td>Tags associated with current device.</td></tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>(optional) method success callback.</td></tr>
<tr class="even"><td>function</td><td><b>fail</b></td><td>(optional) method failure callback.</td></tr>
</tbody>
</table>

Example:

```js
pushNotification.setTags({ "string_tag" : "Hello world", "int_tag" : 42, "list_tag":["hello", "world"]});
```


### getTags

```js
getTags(success, fail)
```

Get tags associated with current device and application.

<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>(optional) method success callback. Receives object containing tags as parameter.</td></tr>
<tr class="even"><td>function</td><td><b>fail</b></td><td>(optional) method failure callback.</td></tr>
</tbody>
</table>

Example:

```js
Pushwoosh.getTags(
    function(tags)
    {
        console.warn('tags for the device: ' + JSON.stringify(tags));
    },
    function(error)
    {
        console.warn('get tags error: ' + JSON.stringify(error));
    }
);
```


### getPushToken

```js
getPushToken(success)
```

Returns push token or null if device is not registered for push notifications.

<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>Method success callback. Receives push token as parameter.</td></tr>
</tbody>
</table>


### getHwid

```js
getHwid(success)
```

Returns Pushwoosh HWID used for communications with Pushwoosh API.

<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>Method success callback. Receives Pushwoosh HWID as parameter.</td></tr>
</tbody>
</table>


### setUserId

```js
setUserId(userId, success, fail)
```

Set User indentifier. This could be Facebook ID, username or email, or any other user ID.
This allows data and events to be matched across multiple user devices.

<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>string</td><td><b>userId</b></td><td>Identifier of currently logged in user</td></tr>
<tr class="even"><td>function</td><td><b>success</b></td><td>(optional) method success callback.</td></tr>
<tr class="even"><td>function</td><td><b>fail</b></td><td>(optional) method failure callback.</td></tr>
</tbody>
</table>


### postEvent

```js
postEvent(event, attributes)
```

Post events for In-App Messages. This can trigger In-App message display as specified in Pushwoosh Control Panel.

<table width=100% style='background-color:#0EA7ED;'>
<colgroup>
<col width="10%" />
<col width="20%" />
<col width="70%" />
</colgroup>
<tbody>
<tr>
<th align="left" colspan="3"><strong>Parameters</strong></th>
</tr>
<tr class="even"><td>string</td><td><b>event</b></td><td>Event name.</td></tr>
<tr class="even"><td>object</td><td><b>attributes</b></td><td>Additional event data.</td></tr>
</tbody>
</table>


Example:

```js
Pushwoosh.postEvent("buttonPressed", { "buttonNumber" : "4", "buttonLabel" : "Banner" })
```


### setApplicationIconBadgeNumber

```js
setApplicationIconBadgeNumber(badgeNumber)
```
**[android, ios]**  
Set application icon badge number.

|Parameter|Description|
|-|-|
|badgeNumber|Icon badge number|


### getApplicationIconBadgeNumber

```js
getApplicationIconBadgeNumber(callback: Function)
```
**[android, ios]**  
Returns application icon badge number.


### addToApplicationIconBadgeNumber

```js
addToApplicationIconBadgeNumber(badgeNumber: number)
```
**[android, ios]**  
Adds value to application icon badge.

|Parameter|Description|
|-|-|
|badgeNumber|Incremental icon badge number|


### setMultiNotificationMode

```js
setMultiNotificationMode(on: boolean)
```
**[android]**  
Allows multiple notifications to be displayed in the Android Notification Center.

|Parameter|Description|
|-|-|
|on|Enable/disable multiple notifications (is disabled by default)|


### setLightScreenOnNotification

```js
setLightScreenOnNotification(on: boolean)
```
**[android]**  
Turns screen on when notification arrives.

|Parameter|Description|
|-|-|
|on|Enable/disable screen unlock (is disabled by default)|


### setEnableLED

```js
setEnableLED(on: boolean)
```
**[android]**  
Enables LED blinking when notification arrives and display is off.

|Parameter|Description|
|-|-|
|on|Enable/disable LED blink (disabled by default)|


### setColorLED

```js
setColorLED(color: number)
```
**[android]**  
Set led color. Use with [setEnableLED](#setenableled).

|Parameter|Description|
|-|-|
|color|LED color in ARGB integer format|


### setSoundType

```js
setSoundType(type: number)
```
**[android]**  
Sets default sound for incoming push notifications.

|Parameter|Description|
|-|-|
|type|Sound type (0 – default, 1 – no sound, 2 – always)|


### setVibrateType

```js
setVibrateType(type: number)
```
**[android]**  
Sets default vibration mode for incoming push notifications.

|Parameter|Description|
|-|-|
|type|Vibration type (0 – default, 1 – no vibration, 2 – always)|

### presentInboxUI

```js
presentInboxUI()
```

Opens [Inbox](https://www.pushwoosh.com/docs/message-inbox) screen.

Before using Message Inbox, please add node_modules/pushwoosh-react-native-plugin/src/ios/PushwooshInboxBundle.bundle to your project. Or, launch a script 'node node_modules/pushwoosh-react-native-plugin/scripts/add_inbox_ios_resources.js' to do it.



### setCommunicationEnabled

```js
setCommunicationEnabled(on: boolean)
```
**[android, ios]**  
A binary method enabling/disabling all communication with Pushwoosh. The boolean value is false unsubscribes the device from receiving push notifications and stops in-app messages download. The value true reverses the effect.



### enableHuaweiPushNotifications

```js
enableHuaweiPushNotifications()
```
**[android]**  
Enables Huawei push messaging. Requires configured Huawei platform for your Pushwoosh application. See the <a href="https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/android-push-notifications/huawei-integration/huawei-in-react-native">integration guide</a>.

### registerSMSNumber

```js
registerSMSNumber(phoneNumber)
```
**[android, ios]**  
Registers phone number associated to the current user.
SMS numbers must be in E.164 format (e.g., "+1234567890") and be valid.

### registerWhatsappNumber

```js
registerWhatsappNumber(phoneNumber)
```
**[android, ios]**  
Registers WhatsApp number associated to the current user.
WhatsApp numbers must be in E.164 format (e.g., "+1234567890") and be valid.

### pushReceived

```js
DeviceEventEmitter.addListener('pushReceived', (e: Event) => {
  console.warn("pushReceived: " + JSON.stringify(e));
  // shows a push is received. Implement passive reaction to a push, such as UI update or data download.
});
```
**[android, ios]**  
Push notification received event is fired when push is received in app.


### pushOpened

```js
DeviceEventEmitter.addListener('pushOpened', (e: Event) => {
  console.warn("pushOpened: " + JSON.stringify(e));
  // shows a user tapped the notification. Implement user interaction, such as showing push details.
});
```
**[android, ios]**  
Push notification opened event is fired when user clicks on push notification.
