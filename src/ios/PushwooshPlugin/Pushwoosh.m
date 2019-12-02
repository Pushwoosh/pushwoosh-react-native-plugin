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
#import <React/RCTConvert.h>
#import <Pushwoosh/PWInbox.h>

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

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

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
        NSString *link = gStartPushData[@"l"];
        
        //get deeplink from the payload and write it to the launchOptions for proper RCTLinking behavior
        if (link) {
            NSMutableDictionary *launchOptions = self.bridge.launchOptions.mutableCopy;
            launchOptions[UIApplicationLaunchOptionsURLKey] = [NSURL URLWithString:link];
            [self.bridge setValue:launchOptions forKey:@"launchOptions"];
        }
        
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

RCT_EXPORT_METHOD(unregister:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
    [[PushNotificationManager pushManager] unregisterForPushNotificationsWithCompletion:^(NSError *error) {
        if (!error && successCallback) {
            successCallback(@[]);
        }
        
        if (error && errorCallback) {
            errorCallback(@[ objectOrNull([error localizedDescription]) ]);
        }
    }];
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

RCT_EXPORT_METHOD(setShowPushnotificationAlert:(BOOL)showPushnotificationAlert) {
    [[PushNotificationManager pushManager] setShowPushnotificationAlert:showPushnotificationAlert];
}

RCT_EXPORT_METHOD(getShowPushnotificationAlert:(RCTResponseSenderBlock)callback) {
    if(callback) {
        callback(@[ @([PushNotificationManager pushManager].showPushnotificationAlert) ]);
    }
}

RCT_EXPORT_METHOD(setUserId:(NSString*)userId) {
	[[PWInAppManager sharedManager] setUserId:userId];
}

RCT_EXPORT_METHOD(postEvent:(NSString*)event withAttributes:(NSDictionary*)attributes) {
	[[PWInAppManager sharedManager] postEvent:event withAttributes:attributes];
}

RCT_EXPORT_METHOD(setApplicationIconBadgeNumber:(nonnull NSNumber*)badgeNumber) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIApplication sharedApplication].applicationIconBadgeNumber = [badgeNumber integerValue];
    });
}

RCT_EXPORT_METHOD(getApplicationIconBadgeNumber:(RCTResponseSenderBlock)callback) {
	if(callback) {
        dispatch_async(dispatch_get_main_queue(), ^{
           callback(@[ @([UIApplication sharedApplication].applicationIconBadgeNumber) ]);
        });
	}
}

RCT_EXPORT_METHOD(addToApplicationIconBadgeNumber:(nonnull NSNumber*)badgeNumber) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIApplication sharedApplication].applicationIconBadgeNumber += [badgeNumber integerValue];
    });
}
    
RCT_EXPORT_METHOD(presentInboxUI:(NSDictionary *)styleDictionary) {
    NSString *resourceBundlePath = [[NSBundle mainBundle] pathForResource:@"PushwooshInboxBundle" ofType:@"bundle"];
    if (![NSBundle bundleWithPath:resourceBundlePath]) {
        NSLog(@"[Pushwoosh][presentInboxUI] Error: PushwooshInboxBundle.bundle not found. Please launch \"node node_modules/pushwoosh-react-native-plugin/scripts/add_inbox_ios_resources.js\" from project root directory or manually add node_modules/pushwoosh-react-native-plugin/src/ios/PushwooshInboxBundle.bundle to your project.");
    } else {
        [[NSOperationQueue mainQueue] addOperationWithBlock:^{
            PWIInboxViewController *inboxViewController = [PWIInboxUI createInboxControllerWithStyle:[self inboxStyleForDictionary:styleDictionary]];
            inboxViewController.navigationItem.leftBarButtonItem = [[UIBarButtonItem alloc] initWithTitle:NSLocalizedString(@"Close", @"Close") style:UIBarButtonItemStylePlain target:self action:@selector(closeInbox)];
            [[Pushwoosh findRootViewController] presentViewController:[[UINavigationController alloc] initWithRootViewController:inboxViewController] animated:YES completion:nil];
            
            __weak typeof (self) wself = self;
            inboxViewController.onMessageClickBlock = ^(NSObject<PWInboxMessageProtocol> *message) {
                if (message.type == PWInboxMessageTypeDeeplink) {
                    [wself closeInbox];
                }
            };
        }];
    }
}

- (PWIInboxStyle *)inboxStyleForDictionary:(NSDictionary *)styleDictionary {
    PWIInboxStyle *style = [PWIInboxStyle defaultStyle];
    
    NSDictionary *defaultImageDict = styleDictionary[@"defaultImageIcon"];
    
    if (defaultImageDict) {
        style.defaultImageIcon = [RCTConvert UIImage:defaultImageDict];
    }
    
    NSString *dateFormat = styleDictionary[@"dateFormat"];
    
    if (dateFormat) {
        style.dateFormatterBlock = ^NSString *(NSDate *date, NSObject *owner) {
            NSDateFormatter *formatter = [NSDateFormatter new];
            formatter.dateFormat = dateFormat;
            return [formatter stringFromDate:date];
        };
    }
    
    NSDictionary *listErrorImageDict = styleDictionary[@"listErrorImage"];
    
    if (listErrorImageDict) {
        style.listErrorImage = [RCTConvert UIImage:listErrorImageDict];
    }
    
    NSDictionary *listEmptyImageDict = styleDictionary[@"listEmptyImage"];
    
    if (listEmptyImageDict) {
        style.listEmptyImage = [RCTConvert UIImage:listEmptyImageDict];
    }
    
    NSDictionary *unreadImageDict = styleDictionary[@"unreadImage"];
    
    if (unreadImageDict) {
        style.unreadImage = [RCTConvert UIImage:unreadImageDict];
    }
    
    NSString *listErrorMessage = styleDictionary[@"listErrorMessage"];
    
    if (listErrorMessage) {
        style.listErrorMessage = listErrorMessage;
    }
    
    NSString *listEmptyMessage = styleDictionary[@"listEmptyMessage"];
    
    if (listEmptyMessage) {
        style.listEmptyMessage = listEmptyMessage;
    }
    
    NSNumber *accentColorValue = styleDictionary[@"accentColor"];
    
    if (accentColorValue) {
        style.accentColor = [RCTConvert UIColor:accentColorValue];
    }
    
    NSNumber *defaultTextColorValue = styleDictionary[@"defaultTextColor"];
    
    if (defaultTextColorValue) {
        style.defaultTextColor = [RCTConvert UIColor:defaultTextColorValue];
    }
    
    NSNumber *backgroundColorValue = styleDictionary[@"backgroundColor"];
    
    if (backgroundColorValue) {
        style.backgroundColor = [RCTConvert UIColor:backgroundColorValue];
    }
    
    if (accentColorValue) {
        style.accentColor = [RCTConvert UIColor:accentColorValue];
    }
    
    NSNumber *highlightColorValue = styleDictionary[@"highlightColor"];
    
    if (highlightColorValue) {
        style.selectionColor = [RCTConvert UIColor:highlightColorValue];
    }
    
    NSNumber *titleColorValue = styleDictionary[@"titleColor"];
    
    if (titleColorValue) {
        style.titleColor = [RCTConvert UIColor:titleColorValue];
    }
    
    NSNumber *descriptionColorValue = styleDictionary[@"descriptionColor"];
    
    if (descriptionColorValue) {
        style.descriptionColor = [RCTConvert UIColor:descriptionColorValue];
    }
    
    NSNumber *dateColorValue = styleDictionary[@"dateColor"];
    
    if (dateColorValue) {
        style.dateColor = [RCTConvert UIColor:dateColorValue];
    }
    
    NSNumber *dividerColorValue = styleDictionary[@"dividerColor"];
    
    if (dividerColorValue) {
        style.separatorColor = [RCTConvert UIColor:dividerColorValue];
    }
    
    NSNumber *barBackgroundColor = styleDictionary[@"barBackgroundColor"];
    
    if (barBackgroundColor) {
        style.barBackgroundColor = [RCTConvert UIColor:barBackgroundColor];
    }
    
    NSNumber *barAccentColor = styleDictionary[@"barAccentColor"];
    
    if (barAccentColor) {
        style.barAccentColor = [RCTConvert UIColor:barAccentColor];
    }
    
    NSNumber *barTextColor = styleDictionary[@"barTextColor"];
    
    if (barTextColor) {
        style.barTextColor = [RCTConvert UIColor:barTextColor];
    }
    
    return style;
}

- (void)closeInbox {
    UIViewController *topViewController = [Pushwoosh findRootViewController];
    if ([topViewController isKindOfClass:[UINavigationController class]] && [((UINavigationController*)topViewController).viewControllers.firstObject isKindOfClass:[PWIInboxViewController class]]) {
        [topViewController dismissViewControllerAnimated:YES completion:nil];
    }
}

+ (UIViewController*)findRootViewController {
    UIApplication *sharedApplication = [UIApplication valueForKey:@"sharedApplication"];
    UIViewController *controller = sharedApplication.keyWindow.rootViewController;
    
    while (controller.presentedViewController) {
        controller = controller.presentedViewController;
    }
    return controller;
}

RCT_EXPORT_METHOD(showGDPRConsentUI) {
    [[PWGDPRManager sharedManager] showGDPRConsentUI];
}

RCT_EXPORT_METHOD(showGDPRDeletionUI) {
    [[PWGDPRManager sharedManager] showGDPRDeletionUI];
}

RCT_EXPORT_METHOD(isDeviceDataRemoved:(RCTResponseSenderBlock)callback) {
    callback(@[@([PWGDPRManager sharedManager].isDeviceDataRemoved)]);
}

RCT_EXPORT_METHOD(isCommunicationEnabled:(RCTResponseSenderBlock)callback) {
    callback(@[@([PWGDPRManager sharedManager].isCommunicationEnabled)]);
}

RCT_EXPORT_METHOD(isAvailableGDPR:(RCTResponseSenderBlock)callback) {
    callback(@[@([PWGDPRManager sharedManager].isAvailable)]);
}

RCT_EXPORT_METHOD(setCommunicationEnabled:(BOOL)enabled success:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
    [[PWGDPRManager sharedManager] setCommunicationEnabled:enabled completion:^(NSError *error) {
        if (error) {
            if (errorCallback) {
                errorCallback(@[error.localizedDescription]);
            }
        } else {
            if (successCallback) {
                successCallback(@[]);
            }
        }
    }];
}

RCT_EXPORT_METHOD(removeAllDeviceData:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
    [[PWGDPRManager sharedManager] removeAllDeviceDataWithCompletion:^(NSError *error) {
        if (error) {
            if (errorCallback) {
                errorCallback(@[error.localizedDescription]);
            }
        } else {
            if (successCallback) {
                successCallback(@[]);
            }
        }
    }];
}

RCT_EXPORT_METHOD(createLocalNotification:(NSDictionary *)params){
    NSString *body = params[@"msg"];
    NSUInteger delay = [params[@"seconds"] unsignedIntegerValue];
    NSDictionary *userData = params[@"userData"];

    [self sendLocalNotificationWithBody:body delay:delay userData:userData];
}

- (void)sendLocalNotificationWithBody:(NSString *)body delay:(NSUInteger)delay userData:(NSDictionary *)userData {
    if (@available(iOS 10, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        UNMutableNotificationContent *content = [UNMutableNotificationContent new];
        content.body = body;
        content.sound = [UNNotificationSound defaultSound];
        content.userInfo = userData;
        UNTimeIntervalNotificationTrigger *trigger = delay > 0 ? [UNTimeIntervalNotificationTrigger triggerWithTimeInterval:delay repeats:NO] : nil;
        NSString *identifier = @"LocalNotification";
        UNNotificationRequest *request = [UNNotificationRequest requestWithIdentifier:identifier
                                                                              content:content
                                                                              trigger:trigger];
        
        [center addNotificationRequest:request withCompletionHandler:^(NSError *_Nullable error) {
            if (error != nil) {
                NSLog(@"Something went wrong: %@", error);
            }
        }];
    } else {
        UILocalNotification *localNotification = [[UILocalNotification alloc] init];
        localNotification.fireDate = [NSDate dateWithTimeIntervalSinceNow:delay];
        localNotification.alertBody = body;
        localNotification.timeZone = [NSTimeZone defaultTimeZone];
        localNotification.userInfo = userData;
        [[UIApplication sharedApplication] scheduleLocalNotification:localNotification];
    }
}

RCT_EXPORT_METHOD(clearLocalNotification){
    if (@available(iOS 10, *)) {
        UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
        [center removeAllDeliveredNotifications];
        [center removeAllPendingNotificationRequests];
    } else{
        [[UIApplication sharedApplication] cancelAllLocalNotifications];
    }
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
    
    [self sendJSEvent:kPushReceivedJSEvent withArgs:pushNotification];
}

- (void)onPushAccepted:(PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart {
	[[PWEventDispatcher sharedDispatcher] dispatchEvent:kPushOpenEvent withArgs:@[ objectOrNull(pushNotification) ]];
	
    [self sendJSEvent:kPushOpenJSEvent withArgs:pushNotification];
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
