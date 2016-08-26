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
```

---
## Method summary
[init(config, success, fail)](#init)  
[register(success, fail)](#register)  
[unregister(success, fail)](#unregister)  
[setTags(tags, success, fail)](#setTags)  
[getTags(success, fail)](#getTags)  
[getPushToken(success)](#getPushToken)  
[getHwid(success)](#getHwid)  
---

### init

Initializes Pushwoosh module with application id and google project number.

```js
init(config, success, fail)
```

* **config.pw_appid** - Pushwoosh application id
* **config.project_number** - GCM project number (for Android push notifications)
* **success** - (optional) initialization success callback
* **fail** - (optional) initialization failure callback


### register

Registers current device for push notifications.

```js
register(success, fail)
```

* **success** - (optional) registration success callback. Receives push token as parameter.
* **fail** - (optional) registration failure callback.

NOTE: if user does not allow application to receive push notifications and `UIBackgroundModes remote-notificaion` is not set in **Info.plist** none of these callbacks will be called.


### unregister

Unregisters current deivce from receiving push notifications.

```js
unregister(success, fail)
```

* **success** - (optional) deregistration success callback
* **fail** - (optional) deregistration failure callback


### setTags

Set tags associated with current device and application.

```js
setTags(tags, success, fail)
```

* **tags** - object containing device tags
* **success** - (optional) method success callback
* **fail** - (optional) method failure callback

Example:

```js
pushNotification.setTags({ "string_tag" : "Hello world", "int_tag" : 42, "list_tag":["hello", "world"]});
```


### getTags

Get tags associated with current device and application.

```js
getTags(success, fail)
```

* **success** - method success callback. Receives object containing tags as parameter.
* **fail** - (optional) method failure callback. 

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

Returns push token or null if device is not registered for push notifications.

```js
getPushToken(success)
```

* **success** - method success callback. Receives push token as parameter.


### getHwid

Returns Pushwoosh HWID used for communications with Pushwoosh API.

```js
getHwid(success)
```

* **success** - method success callback. Receives Pushwoosh HWID as parameter.


### setUserId

Set User indentifier. This could be Facebook ID, username or email, or any other user ID.
This allows data and events to be matched across multiple user devices.

```js
setUserId(userId)
```


### postEvent

Post events for In-App Messages. This can trigger In-App message display as specified in Pushwoosh Control Panel.

```js
postEvent(event, attributes)
```

* **event** - event to trigger
* **attributes** - object with additional event attributes
