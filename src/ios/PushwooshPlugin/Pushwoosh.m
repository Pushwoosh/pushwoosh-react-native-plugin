//
//  Pushwoosh.m
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import "Pushwoosh.h"

#import <React/RCTUtils.h>
#import <React/RCTBridge.h>
#import "PWEventDispatcher.h"
#import <React/RCTEventDispatcher.h>

#import <UserNotifications/UserNotifications.h>

static id objectOrNull(id object) {
    if (object) {
        return object;
    } else {
        return [NSNull null];
    }
}

static NSDictionary * gStartPushData = nil;
static NSString * const kRegistrationSuccesEvent = @"PWRegistrationSuccess";
static NSString * const kRegistrationErrorEvent = @"PWRegistrationError";
static NSString * const kPushReceivedEvent = @"PWPushReceived";
static NSString * const kPushOpenEvent = @"PWPushOpen";

static NSString * const kPushOpenJSEvent = @"pushOpened";
static NSString * const kPushReceivedJSEvent = @"pushReceived";

@implementation Pushwoosh

#pragma mark - Pushwoosh RCTBridgeModule

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(init:(NSDictionary*)config success:(RCTResponseSenderBlock)success error:(RCTResponseSenderBlock)error) {
	NSString *appCode = config[@"pw_appid"];
	
	if (!appCode || ![appCode isKindOfClass:[NSString class]]) {
		if (error) {
			error(@[ @"pw_appid is missing" ]);
		}
		
		return;
	}
	
	[PushNotificationManager initializeWithAppCode:appCode appName:nil];
	[[PushNotificationManager pushManager] sendAppOpen];
	[PushNotificationManager pushManager].delegate = self;
	[UNUserNotificationCenter currentNotificationCenter].delegate = [PushNotificationManager pushManager].notificationCenterDelegate;
	
    if (success) {
        success(@[]);
    }
    
	if (gStartPushData) {
        [self sendJSEvent:kPushReceivedJSEvent withArgs:gStartPushData];
		[self sendJSEvent:kPushOpenJSEvent withArgs:gStartPushData];
    } else if([PushNotificationManager pushManager].launchNotification) {
        [self sendJSEvent:kPushReceivedJSEvent withArgs:[PushNotificationManager pushManager].launchNotification];
        [self sendJSEvent:kPushOpenJSEvent withArgs:[PushNotificationManager pushManager].launchNotification];
    }
}

RCT_EXPORT_METHOD(register:(RCTResponseSenderBlock)success error:(RCTResponseSenderBlock)error) {
	[[PWEventDispatcher sharedDispatcher] subscribe:success toEvent:kRegistrationSuccesEvent];
	[[PWEventDispatcher sharedDispatcher] subscribe:error toEvent:kRegistrationErrorEvent];
	
	[[PushNotificationManager pushManager] registerForPushNotifications];
}

RCT_EXPORT_METHOD(unregister:(RCTResponseSenderBlock)success error:(RCTResponseSenderBlock)error) {
	[[PushNotificationManager pushManager] unregisterForPushNotifications];
	if (success) {
		success(@[]);
	}
}

RCT_EXPORT_METHOD(onPushOpen:(RCTResponseSenderBlock)callback) {
	[[PWEventDispatcher sharedDispatcher] subscribe:callback toEvent:kPushOpenEvent];
	
	if (gStartPushData) {
		NSDictionary *pushData = gStartPushData;
		gStartPushData = nil;
		[[PWEventDispatcher sharedDispatcher] dispatchEvent:kPushOpenEvent withArgs:@[ objectOrNull(pushData) ]];
	}
}

RCT_EXPORT_METHOD(onPushReceived:(RCTResponseSenderBlock)callback) {
    [[PWEventDispatcher sharedDispatcher] subscribe:callback toEvent:kPushReceivedEvent];
    
    if (gStartPushData) {
        NSDictionary *pushData = gStartPushData;
        gStartPushData = nil;
        [[PWEventDispatcher sharedDispatcher] dispatchEvent:kPushReceivedEvent withArgs:@[ objectOrNull(pushData) ]];
    }
}

RCT_EXPORT_METHOD(getHwid:(RCTResponseSenderBlock)callback) {
	if (callback) {
		callback(@[ [[PushNotificationManager pushManager] getHWID] ]);
	}
}

RCT_EXPORT_METHOD(getPushToken:(RCTResponseSenderBlock)callback) {
	if (callback) {
		callback(@[ objectOrNull([[PushNotificationManager pushManager] getPushToken]) ]);
	}
}

RCT_EXPORT_METHOD(setTags:(NSDictionary*)tags success:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
	[[PushNotificationManager pushManager] setTags:tags withCompletion:^(NSError* error) {
		if (!error && successCallback) {
			successCallback(@[]);
		}
		
		if (error && errorCallback) {
			errorCallback(@[ objectOrNull([error localizedDescription]) ]);
		}
	}];
}

RCT_EXPORT_METHOD(getTags:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
	[[PushNotificationManager pushManager] loadTags:^(NSDictionary* tags) {
		if (successCallback) {
			successCallback(@[ tags ]);
		}
	} error:^(NSError *error) {
		if (errorCallback) {
			errorCallback(@[ objectOrNull([error localizedDescription]) ]);
		}
	}];
}

RCT_EXPORT_METHOD(setShowPushnotificationAlert:(BOOL *)showPushnotificationAlert) {
    [[PushNotificationManager pushManager] setShowPushnotificationAlert:showPushnotificationAlert];
}

RCT_EXPORT_METHOD(getShowPushnotificationAlert:(RCTResponseSenderBlock)callback) {
    if(callback) {
        callback(@[ @([PushNotificationManager pushManager].showPushnotificationAlert) ]);
    }
}

RCT_EXPORT_METHOD(setUserId:(NSString*)userId) {
	[[PushNotificationManager pushManager] setUserId:userId];
}

RCT_EXPORT_METHOD(postEvent:(NSString*)event withAttributes:(NSDictionary*)attributes) {
	[[PushNotificationManager pushManager] postEvent:event withAttributes:attributes];
}

RCT_EXPORT_METHOD(startLocationTracking) {
	[[PushNotificationManager pushManager] startLocationTracking];
}

RCT_EXPORT_METHOD(stopLocationTracking) {
	[[PushNotificationManager pushManager] stopLocationTracking];
}

RCT_EXPORT_METHOD(setApplicationIconBadgeNumber:(nonnull NSNumber*)badgeNumber) {
	[UIApplication sharedApplication].applicationIconBadgeNumber = [badgeNumber integerValue];
}

RCT_EXPORT_METHOD(getApplicationIconBadgeNumber:(RCTResponseSenderBlock)callback) {
	if(callback) {
		callback(@[ @([UIApplication sharedApplication].applicationIconBadgeNumber) ]);
	}
}

RCT_EXPORT_METHOD(addToApplicationIconBadgeNumber:(nonnull NSNumber*)badgeNumber) {
	[UIApplication sharedApplication].applicationIconBadgeNumber += [badgeNumber integerValue];
}

#pragma mark - PushNotificationDelegate

- (void)onDidRegisterForRemoteNotificationsWithDeviceToken:(NSString *)token {
	[[PWEventDispatcher sharedDispatcher] dispatchEvent:kRegistrationSuccesEvent withArgs:@[ objectOrNull(token) ]];
}

- (void)onDidFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
	[[PWEventDispatcher sharedDispatcher] dispatchEvent:kRegistrationErrorEvent withArgs:@[ objectOrNull([error localizedDescription]) ]];
}

- (void)onPushReceived:(PushNotificationManager *)pushManager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart {
    [[PWEventDispatcher sharedDispatcher] dispatchEvent:kPushReceivedEvent withArgs:@[ objectOrNull(pushNotification) ]];
    
    if ([[UIApplication sharedApplication] applicationState] != UIApplicationStateBackground) {
        [self sendJSEvent:kPushReceivedJSEvent withArgs:pushNotification];
    }
}

- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart {
	[[PWEventDispatcher sharedDispatcher] dispatchEvent:kPushOpenEvent withArgs:@[ objectOrNull(pushNotification) ]];
	
	if ([[UIApplication sharedApplication] applicationState] != UIApplicationStateBackground) {
		[self sendJSEvent:kPushOpenJSEvent withArgs:pushNotification];
	}
}

#pragma mark - RCTEventEmitter

- (void)sendJSEvent:(NSString*)event withArgs:(NSDictionary*)args {
//	[self sendEventWithName:event body:args];
	[self.bridge.eventDispatcher sendDeviceEventWithName:event body:args];
}

- (NSArray<NSString *> *)supportedEvents {
	return @[ kPushOpenJSEvent ];
}

@end

@implementation UIApplication (InternalPushRuntime)

- (BOOL)pushwooshUseRuntimeMagic {
	return YES;
}

// Just keep the launch notification until the module starts and callback functions initalizes
- (void)onPushReceived:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart {
    if (onStart) {
        gStartPushData = pushNotification;
    }
}
- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart {
	if (onStart) {
		gStartPushData = pushNotification;
	}
}

- (NSObject<PushNotificationDelegate> *)getPushwooshDelegate {
	return (NSObject<PushNotificationDelegate> *)self;
}

@end
