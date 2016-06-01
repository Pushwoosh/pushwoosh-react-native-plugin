'use strict';

import { NativeModules } from 'react-native';

var PushwooshModule = NativeModules.Pushwoosh;

function PushNotification() {}

//Function: init
//Call this first thing with your Pushwoosh App ID (pw_appid parameter) and Google Project ID for Android (projectid parameter)
//
//Example:
//(start code)
//	//initialize Pushwoosh with projectid: "GOOGLE_PROJECT_ID", appid : "PUSHWOOSH_APP_ID". This will trigger all pending push notifications on start.
//	Pushwoosh.init({ projectid: "XXXXXXXXXXXXXXX", pw_appid : "XXXXX-XXXXX" });
//(end)
PushNotification.prototype.init = function(config, success, fail) {
	PushwooshModule.init(config, success, fail);
};

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
PushNotification.prototype.register = function(success, fail) {
	PushwooshModule.register(success, fail);
};

//Function: unregister
//Unregisters device from push notifications
PushNotification.prototype.unregister = function(success, fail) {
	PushwooshModule.unregister(success, fail);
};

//Function: unregister
//Unregisters device from push notifications
PushNotification.prototype.onPushOpen = function(callback) {
	PushwooshModule.onPushOpen(callback);
};

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
//	//setings list tags "MyTag" with values (array) "hello", "world"
//	pushNotification.setTags({"MyTag":["hello", "world"]});
//(end)
PushNotification.prototype.setTags = function(tags, success, fail) {
	PushwooshModule.setTags(tags, success, fail);
};

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
PushNotification.prototype.getTags = function(success, fail) {
	PushwooshModule.getTags(success, fail);
};

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
PushNotification.prototype.getPushToken = function(success) {
	PushwooshModule.getPushToken(success);
};

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
PushNotification.prototype.getHwid = function(success) {
	PushwooshModule.getHwid(success);
};

module.exports = new PushNotification();