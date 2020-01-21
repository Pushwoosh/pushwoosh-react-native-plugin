'use strict';

import { NativeModules } from 'react-native';

const PushwooshModule = NativeModules.Pushwoosh;

//Class: PushNotification
//Use `PushNotification` to register device for push notifications on Pushwoosh and customize notification appearance.
//
//Example:
//(start code)
//DeviceEventEmitter.addListener('pushOpened', (e: Event) => {
//  console.warn("pushOpened: " + JSON.stringify(e));
//  alert(JSON.stringify(e));
//});
//
//const Pushwoosh = require('pushwoosh-react-native-plugin');
//
//Pushwoosh.init({ "pw_appid" : "XXXX-XXXX", "project_number" : "XXXXXXXXXXXXX" });
//
//Pushwoosh.register(
//  (token) => {
//    console.warn("Registered for pushes: " + token);
//  },
//  (error) => {
//    console.warn("Failed to register: " + error);
//  }
//);
//(end)
class PushNotification {

	//Function: init
	//Call this first thing with your Pushwoosh App ID (pw_appid parameter) and Google Project ID for Android (projectid parameter)
	//
	//Example:
	//(start code)
	//	//initialize Pushwoosh with projectid: "GOOGLE_PROJECT_ID", appid : "PUSHWOOSH_APP_ID". This will trigger all pending push notifications on start.
	//	Pushwoosh.init({ projectid: "XXXXXXXXXXXXXXX", pw_appid : "XXXXX-XXXXX" });
	//(end)
	init(config: Object, success: ?Function, fail: ?Function) {
		if (!success) {
			success = function() {};
		}
		if (!fail) {
			fail = function(error) {};
		}
		PushwooshModule.init(config, success, fail);
	}

    //Function: createLocalNotification
	//Creates a local notification with a specified message, delay and custom data
	//
	//Example:
	//(start code)
	//	//creates a local notification with "message" content, 5 seconds delay and passes {"somedata":"optional"} object in payload
	//	Pushwoosh.createLocalNotification({msg:"message", seconds:5, userData:{"somedata":"optional"}});
	//(end)

	createLocalNotification(data: Object){
		PushwooshModule.createLocalNotification(data);
	}

    //Function: clearLocalNotification
	//Clears all existing and cancels all pending local notifications
	//
	//Example:
	//(start code)
	//	Pushwoosh.clearLocalNotification();
	//(end)

	clearLocalNotification(){
		PushwooshModule.clearLocalNotification();
	}

	//Function: register
	//Call this to register for push notifications and retreive a push Token
	//
	//Example:
	//(start code)
	//	Pushwoosh.registerDevice(
	//		function(token)
	//		{
	//			alert(token);
	//		},
	//		function(status)
	//		{
	//			alert("failed to register: " +  status);
	//		}
	//	);
	//(end)
	register(success: ?Function, fail: ?Function) {
		if (!success) {
			success = function(token) {};
		}
		if (!fail) {
			fail = function(error) {};
		}
		PushwooshModule.register(success, fail);
	}

	//Function: unregister
	//Unregisters device from push notifications
	unregister(success: ?Function, fail: ?Function) {
		if (!success) {
			success = function(token) {};
		}
		if (!fail) {
			fail = function(error) {};
		}
		PushwooshModule.unregister(success, fail);
	}

	//Function: onPushOpen
	//Deprecated - use DeviceEventEmitter.addListener('pushOpened', callback) instead
	onPushOpen(callback: Function) {
		PushwooshModule.onPushOpen(callback);
	}

	//Function: setTags
	//Call this to set tags for the device
	//
	//Example:
	//sets the following tags: "deviceName" with value "hello" and "deviceId" with value 10
	//(start code)
	//	Pushwoosh.setTags({deviceName:"hello", deviceId:10},
	//		function(status) {
	//			console.warn('setTags success');
	//		},
	//		function(status) {
	//			console.warn('setTags failed');
	//		}
	//	);
	//
	//	//settings list tags "MyTag" with values (array) "hello", "world"
	//	pushNotification.setTags({"MyTag":["hello", "world"]});
	//(end)
	setTags(tags: Object, success: ?Function, fail: ?Function) {
		if (!success) {
			success = function() {};
		}
		if (!fail) {
			fail = function(error) {};
		}
		PushwooshModule.setTags(tags, success, fail);
	}

	//Function: getTags
	//Call this to get tags for the device
	//
	//Example:
	//(start code)
	//	Pushwoosh.getTags(
	//		function(tags)
	//		{
	//			console.warn('tags for the device: ' + JSON.stringify(tags));
	//		},
	//		function(error)
	//		{
	//			console.warn('get tags error: ' + JSON.stringify(error));
	//		}
	//	);
	//(end)
	getTags(success: Function, fail: ?Function) {
		if (!fail) {
			fail = function(error) {};
		}
		PushwooshModule.getTags(success, fail);
	}

    //Function: setShowPushnotificationAlert
    //Set push notifications alert when push notification is received while the app is running, default is `true`
    //
    //Example:
    //(start code)
    //    Pushwoosh.setShowPushnotificationAlert(false);
    //(end)
    setShowPushnotificationAlert(showPushnotificationAlert: boolean) {
        PushwooshModule.setShowPushnotificationAlert(showPushnotificationAlert);
    }
    
    //Function: getShowPushnotificationAlert
    //Show push notifications alert when push notification is received while the app is running, default is `true`
    //
    //Example:
    //(start code)
    //    Pushwoosh.getShowPushnotificationAlert((showPushnotificationAlert) => {
    //                                           console.warn("showPushnotificationAlert = " + showPushnotificationAlert);
    //                                           });
    //(end)
    getShowPushnotificationAlert(callback: Function) {
        PushwooshModule.getShowPushnotificationAlert(callback);
    }
    
	//Function: getPushToken
	//Call this to get push token if it is available. Note the token also comes in registerDevice function callback.
	//
	//Example:
	//(start code)
	//	Pushwoosh.getPushToken(
	//		function(token)
	//		{
	//			console.warn('push token: ' + token);
	//		}
	//	);
	//(end)
	getPushToken(success: Function) {
		PushwooshModule.getPushToken(success);
	}

	//Function: getHwid
	//Call this to get Pushwoosh HWID used for communications with Pushwoosh API
	//
	//Example:
	//(start code)
	//	Pushwoosh.getHwid(
	//		function(token) {
	//			console.warn('Pushwoosh HWID: ' + token);
	//		}
	//	);
	//(end)
	getHwid(success: Function) {
		PushwooshModule.getHwid(success);
	}

	//Function: setUserId
	//[android, ios] Set User indentifier. This could be Facebook ID, username or email, or any other user ID.
	//This allows data and events to be matched across multiple user devices.
	//
	//Parameters:
	// "userId" - user string identifier
	//
	setUserId(userId: string) {
		PushwooshModule.setUserId(userId);
	}

	//Function: postEvent
	//[android, ios] Post events for In-App Messages. This can trigger In-App message display as specified in Pushwoosh Control Panel.
	//
	//Parameters:
	// "event" - event to trigger
	// "attributes" - object with additional event attributes
	// 
	// Example:
	//(start code)
	// Pushwoosh.setUserId("XXXXXX");
	// Pushwoosh.postEvent("buttonPressed", { "buttonNumber" : 4, "buttonLabel" : "banner" });
	//(end)
	postEvent(event: string, attributes: ?Object) {
		if (!attributes) {
			attributes = {};
		}
		PushwooshModule.postEvent(event, attributes);
	}

	//Function: setApplicationIconBadgeNumber
	//[android, ios, wp8, windows] Set the application icon badge number
	//
	//Parameters:
	// "badgeNumber" - icon badge number
	//
	setApplicationIconBadgeNumber(badgeNumber: number) {
		PushwooshModule.setApplicationIconBadgeNumber(badgeNumber);
	}

	//Function: getApplicationIconBadgeNumber
	//[android, ios] Returns the application icon badge number
	//
	//Parameters:
	// "callback" - success callback
	//
	//Example:
	//(start code)
	//	Pushwoosh.getApplicationIconBadgeNumber(function(badge){ alert(badge);} );
	//(end)
	getApplicationIconBadgeNumber(callback: Function) {
		PushwooshModule.getApplicationIconBadgeNumber(callback);
	}
	
	//Function: addToApplicationIconBadgeNumber
	//[android, ios] Adds value to the application icon badge
	//
	//Parameters:
	// "badgeNumber" - incremental icon badge number
	//
	//Example:
	//(start code)
	//	Pushwoosh.addToApplicationIconBadgeNumber(5);
	//	Pushwoosh.addToApplicationIconBadgeNumber(-5);
	//(end)
	addToApplicationIconBadgeNumber(badgeNumber: number) {
		PushwooshModule.addToApplicationIconBadgeNumber(badgeNumber);
	}

	//Function: setMultiNotificationMode
	//[android] Allows multiple notifications to be displayed in the Android Notification Center
	setMultiNotificationMode(on: boolean) {
		PushwooshModule.setMultiNotificationMode(on);
	}

	//Function: setLightScreenOnNotification
	//[android] Turns the screen on if notification arrives
	//
	//Parameters:
	// "on" - enable/disable screen unlock (is disabled by default)
	//
	setLightScreenOnNotification(on: boolean) {
		PushwooshModule.setLightScreenOnNotification(on);
	}

	//Function: setEnableLED
	//[android] Enables led blinking when notification arrives and display is off
	//
	//Parameters:
	// "on" - enable/disable led blink (is disabled by default)
	//
	setEnableLED(on: boolean) {
		PushwooshModule.setEnableLED(on);
	}

	//Function: setEnableLED
	//[android] Set led color. Use with <setEnableLED>
	//
	//Parameters:
	// "color" - led color in ARGB integer format
	//
	setColorLED(color: number) {
		PushwooshModule.setColorLED(color);
	}

	//Function: setSoundType
	//[android] Sets default sound to play when push notification arrive.
	//
	//Parameters:
	// "type" - Sound type (0 - default, 1 - no sound, 2 - always)
	//
	setSoundType(type: number) {
		PushwooshModule.setSoundType(type);
	}

	//Function: setVibrateType
	//[android] Sets default vibration mode when push notification arrive.
	//
	//Parameters:
	// "type" - Vibration type (0 - default, 1 - no vibration, 2 - always)
	//
	setVibrateType(type: number) {
		PushwooshModule.setVibrateType(type);
	}

	//Function: presentInboxUI
	//[android, ios] Opens Inbox screen.
	//
	// Supported style keys:
	//
	// Customizes the date formatting
	// "dateFormat"
	//
	// The default icon in the cell next to the message; if not specified, the app icon is used
	// "defaultImageIcon"
	//
	// The appearance of the unread messages mark (iOS only)
	// "unreadImage"
	//
	// The image which is displayed if an error occurs and the list of inbox messages is empty
	// "listErrorImage"
	//
	// The image which is displayed if the list of inbox messages is empty
	// "listEmptyImage"
	//
	// The error text which is displayed when an error occurs; cannot be localized
	// "listErrorMessage"
	//
	// The text which is displayed if the list of inbox messages is empty; cannot be localized
	// "listEmptyMessage"
	//
	// The default text color (iOS only)
	// "defaultTextColor"
	//
	// The accent color
	// "accentColor"
	//
	// The default background color
	// "backgroundColor"
	//
	// The default selection color
	// "highlightColor"
	//
	// The color of message titles
	// "titleColor"
	//
	// The color of message titles if message was readed (Android only)
	// "readTitleColor"
	//
	// The color of messages descriptions
	// "descriptionColor"
	//
	// The color of messages descriptions if message was readed (Android only)
	// "readDescriptionColor"
	//
	// The color of message dates
	// "dateColor"
	//
	// The color of message dates if message was readed (Android only)
	// "readDateColor"
	//
	// The color of the separator
	// "dividerColor"
	//
	//Example:
	//(start code)
	//	Pushwoosh.presentInboxUI({ 
	//   "dateFormat" : "dd.MMMM.YYYY",
	//   "defaultImageIcon" : Image.resolveAssetSource(require('./icon.png')),
	//   "listErrorImage" : Image.resolveAssetSource(require('./error.png')),
	//	 "listEmptyImage" : Image.resolveAssetSource(require('./empty.png')),
	//   "listErrorMessage" : "Error message1",
	//   "listEmptyMessage" : "Error message2",
	//   "accentColor" : processColor('#ff00ff'),
	//   "highlightColor" : processColor('yellow'),
	//   "dateColor" : processColor('blue'),
	//   "titleColor" : processColor('#ff00ff'),
	//   "dividerColor" : processColor('#ff00ff'),
	//   "descriptionColor" : processColor('green'),
	//   "backgroundColor" : processColor('rgba(255, 100, 30, 1.0)'),
	//   "barBackgroundColor" : processColor('#ffffff'),
	//   "barAccentColor" : processColor('#ffd700'),
	//   "barTextColor" : processColor('#282828')
	// });
	//(end)

	presentInboxUI(style: ?Object) {
		PushwooshModule.presentInboxUI(style);
	}

	// Show inApp for change setting Enable/disable all communication with Pushwoosh
	showGDPRConsentUI(){
		PushwooshModule.showGDPRConsentUI();
	}

	// Show inApp for all device data from Pushwoosh and stops all interactions and communication permanently.
	showGDPRDeletionUI(){
		PushwooshModule.showGDPRDeletionUI();
	}

	isDeviceDataRemoved(success: Function){
		 PushwooshModule.isDeviceDataRemoved(success);
	}

	// Return flag is enable communication with server
	isCommunicationEnabled(success: Function){
		PushwooshModule.isCommunicationEnabled(success);
	}

	// Return flag is enabled GDPR on server	
	isAvailableGDPR(success: Function){
		PushwooshModule.isAvailableGDPR(success);
	}

	// Enable/disable all communication with Pushwoosh. Enabled by default.
	setCommunicationEnabled(enable: boolean, success: ?Function, fail: ?Function) {
		if (!success) {
			success = function() {};
		}
		if (!fail) {
			fail = function(error) {};
		}
		PushwooshModule.setCommunicationEnabled(enable, success, fail);
	}

	// Removes all device data from Pushwoosh and stops all interactions and communication permanently.
	removeAllDeviceData( success: ?Function, fail: ?Function) {
		if (!success) {
			success = function() {};
		}
		if (!fail) {
			fail = function(error) {};
		}
		PushwooshModule.removeAllDeviceData(success, fail);
	}

	// Set notification icon background color
	setNotificationIconBackgroundColor(color: string) {
		PushwooshModule.setNotificationIconBackgroundColor(color);
	}

	// Set custom application language. Must be a lowercase two-letter code according to ISO-639-1 standard ("en", "de", "fr", etc.).
	// Device language used by default.
	// Set to null if you want to use device language again.
	setLanguage(language: string) {
		PushwooshModule.setLanguage(language);
	}
}

module.exports = new PushNotification();
