//
//  Pushwoosh.m
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import "Pushwoosh.h"

#import "RCTUtils.h"
#import "RCTBridge.h"
#import "PWEventDispatcher.h"

static id objectOrNull(id object) {
	if (!object) {
		return [NSNull null];
	}
	return object;
}

static NSDictionary * gStartPushData = nil;
static NSString * const kRegistrationSuccesEvent = @"PWRegistrationSuccess";
static NSString * const kRegistrationErrorEvent = @"PWRegistrationError";
static NSString * const kPushOpenEvent = @"PWPushOpen";

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
	
	if (success) {
		success(@[]);
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

#pragma mark - PushNotificationDelegate

- (void)onDidRegisterForRemoteNotificationsWithDeviceToken:(NSString *)token {
	[[PWEventDispatcher sharedDispatcher] dispatchEvent:kRegistrationSuccesEvent withArgs:@[ objectOrNull(token) ]];
}

- (void)onDidFailToRegisterForRemoteNotificationsWithError:(NSError *)error {
	[[PWEventDispatcher sharedDispatcher] dispatchEvent:kRegistrationErrorEvent withArgs:@[ objectOrNull([error localizedDescription]) ]];
}

- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart {
	[[PWEventDispatcher sharedDispatcher] dispatchEvent:kPushOpenEvent withArgs:@[ objectOrNull(pushNotification) ]];
}

@end

@implementation UIApplication (InternalPushRuntime)

- (BOOL)pushwooshUseRuntimeMagic {
	return YES;
}

// Just keep the launch notification until the module starts and callback functions initalizes
- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart {
	if (onStart) {
		gStartPushData = pushNotification;
	}
}

- (NSObject<PushNotificationDelegate> *)getPushwooshDelegate {
	return (NSObject<PushNotificationDelegate> *)self;
}

@end
