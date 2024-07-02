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

#import <UserNotifications/UserNotifications.h>
#import <Pushwoosh/PushNotificationManager.h>

#import <objc/runtime.h>

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

@interface PushwooshPlugin (InnerPushwooshPlugin)

- (void) application:(UIApplication *)application pwplugin_didRegisterWithDeviceToken:(NSData *)deviceToken;
- (void) application:(UIApplication *)application pwplugin_didReceiveRemoteNotification:(NSDictionary *)userInfo fetchCompletionHandler:(void (^)(UIBackgroundFetchResult))completionHandler;
- (void) application:(UIApplication *)application pwplugin_didFailToRegisterForRemoteNotificationsWithError:(NSError *)error;

@end

void pushwoosh_swizzle(Class class, SEL fromChange, SEL toChange, IMP impl, const char * signature) {
    Method method = nil;
    method = class_getInstanceMethod(class, fromChange);
    
    if (method) {
        //method exists add a new method and swap with original
        class_addMethod(class, toChange, impl, signature);
        method_exchangeImplementations(class_getInstanceMethod(class, fromChange), class_getInstanceMethod(class, toChange));
    } else {
        //just add as orignal method
        class_addMethod(class, fromChange, impl, signature);
    }
}

@implementation PushwooshPlugin

API_AVAILABLE(ios(10))
__weak id<UNUserNotificationCenterDelegate> _originalNotificationCenterDelegate;
API_AVAILABLE(ios(10))
  struct {
    unsigned int willPresentNotification : 1;
    unsigned int didReceiveNotificationResponse : 1;
    unsigned int openSettingsForNotification : 1;
  } _originalNotificationCenterDelegateResponds;

#pragma mark - Pushwoosh RCTBridgeModule

RCT_EXPORT_MODULE(Pushwoosh);

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

RCT_EXPORT_METHOD(init:(NSDictionary*)config success:(RCTResponseSenderBlock)success error:(RCTResponseSenderBlock)error) {
    NSString *appCode = config[@"pw_appid"];
    NSString *notificationHandling = config[@"pw_notification_handling"];
    
    if (!appCode || ![appCode isKindOfClass:[NSString class]]) {
        if (error) {
            error(@[ @"pw_appid is missing" ]);
        }
        
        return;
    }
    
    NSString *proxyUrl = config[@"reverse_proxy_url"];
    if (proxyUrl && ![proxyUrl isEqualToString:@""]) {
        [[Pushwoosh sharedInstance] setReverseProxy:proxyUrl];
    }
    
    [PushNotificationManager initializeWithAppCode:appCode appName:nil];
    [[PushNotificationManager pushManager] sendAppOpen];
    [PushNotificationManager pushManager].delegate = self;

    [PushwooshPlugin swizzleNotificationSettingsHandler];

    // We set Pushwoosh UNUserNotificationCenter delegate unless CUSTOM is specified in the config
    if(![notificationHandling isEqualToString:@"CUSTOM"]) {
        if (@available(iOS 10, *)) {
            BOOL shouldReplaceDelegate = YES;

            UNUserNotificationCenter *notificationCenter = [UNUserNotificationCenter currentNotificationCenter];

#if !TARGET_OS_OSX
            if ([notificationCenter.delegate conformsToProtocol:@protocol(PushNotificationDelegate)]) {
                shouldReplaceDelegate = NO;
            }
#endif
            
            if (notificationCenter.delegate != nil && shouldReplaceDelegate) {
                _originalNotificationCenterDelegate = notificationCenter.delegate;
                _originalNotificationCenterDelegateResponds.openSettingsForNotification =
                (unsigned int)[_originalNotificationCenterDelegate
                               respondsToSelector:@selector(userNotificationCenter:openSettingsForNotification:)];
                _originalNotificationCenterDelegateResponds.willPresentNotification =
                (unsigned int)[_originalNotificationCenterDelegate
                               respondsToSelector:@selector(userNotificationCenter:
                                                            willPresentNotification:withCompletionHandler:)];
                _originalNotificationCenterDelegateResponds.didReceiveNotificationResponse =
                (unsigned int)[_originalNotificationCenterDelegate
                               respondsToSelector:@selector(userNotificationCenter:
                                                            didReceiveNotificationResponse:withCompletionHandler:)];
            }
            
            if (shouldReplaceDelegate) {
                __strong PushwooshPlugin<UNUserNotificationCenterDelegate> *strongSelf = (PushwooshPlugin<UNUserNotificationCenterDelegate> *)self;
                notificationCenter.delegate = (id<UNUserNotificationCenterDelegate>)strongSelf;
            }
        }
    }

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

RCT_EXPORT_METHOD(getUserId:(RCTResponseSenderBlock)callback) {
    if (callback) {
        callback(@[ [[Pushwoosh sharedInstance] getUserId] ]);
    }
}

RCT_EXPORT_METHOD(getPushToken:(RCTResponseSenderBlock)callback) {
    if (callback) {
        callback(@[ objectOrNull([[PushNotificationManager pushManager] getPushToken]) ]);
    }
}

RCT_EXPORT_METHOD(setEmails:(NSArray *)emails success:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
    __block NSError* gError = nil;
    [[Pushwoosh sharedInstance] setEmails:emails completion:^(NSError * _Nullable error) {
        if (error) {
            gError = error;
        }
    }];
    if (!gError && successCallback) {
        successCallback(@[]);
    }
    
    if (gError && errorCallback) {
        errorCallback(@[ objectOrNull([gError localizedDescription]) ]);
    }
}

RCT_EXPORT_METHOD(setUserEmails:(NSString*)userId emails:(NSArray *)emails success:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
    __block NSError* gError = nil;
    [[Pushwoosh sharedInstance] setUser:userId emails:emails completion:^(NSError * _Nullable error) {
        if (error) {
            gError = error;
        }
    }];
    if (!gError && successCallback) {
        successCallback(@[]);
    }
    
    if (gError && errorCallback) {
        errorCallback(@[ objectOrNull([gError localizedDescription]) ]);
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

RCT_EXPORT_METHOD(setUserId:(NSString*)userId success:(RCTResponseSenderBlock)successCallback error:(RCTResponseSenderBlock)errorCallback) {
    
    [[PWInAppManager sharedManager] setUserId:userId completion:^(NSError *error) {
        if (!error && successCallback) {
            successCallback(@[]);
        }
        
        if (error && errorCallback) {
            errorCallback(@[ objectOrNull([error localizedDescription]) ]);
        }
    }];
}

RCT_EXPORT_METHOD(postEvent:(NSString*)event withAttributes:(NSDictionary*)attributes) {
    [[PWInAppManager sharedManager] postEvent:event withAttributes:attributes];
}

RCT_EXPORT_METHOD(setApplicationIconBadgeNumber:(nonnull NSNumber*)badgeNumber) {
    dispatch_async(dispatch_get_main_queue(), ^{
        [UIApplication sharedApplication].applicationIconBadgeNumber = [badgeNumber integerValue];
    });
}

RCT_EXPORT_METHOD(setLanguage:(NSString *)language) {
    [PushNotificationManager pushManager].language = language;
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
            [[PushwooshPlugin findRootViewController] presentViewController:[[UINavigationController alloc] initWithRootViewController:inboxViewController] animated:YES completion:nil];
            
            __weak typeof (self) wself = self;
            inboxViewController.onMessageClickBlock = ^(NSObject<PWInboxMessageProtocol> *message) {
                if (message.type == PWInboxMessageTypeDeeplink) {
                    [wself closeInbox];
                }
            };
        }];
    }
}

RCT_EXPORT_METHOD(messagesWithNoActionPerformedCount:(RCTResponseSenderBlock)callback) {
    [PWInbox messagesWithNoActionPerformedCountWithCompletion:^(NSInteger count, NSError *error) {
        if (callback) {
            callback(@[ @(count) ]);
        }
    }];
}

RCT_EXPORT_METHOD(unreadMessagesCount:(RCTResponseSenderBlock)callback) {
    [PWInbox unreadMessagesCountWithCompletion:^(NSInteger count, NSError *error) {
        if (callback) {
            callback(@[ @(count) ]);
        }
    }];
}

RCT_EXPORT_METHOD(messagesCount:(RCTResponseSenderBlock)callback) {
    [PWInbox messagesCountWithCompletion:^(NSInteger count, NSError *error) {
        if (callback) {
            callback(@[ @(count) ]);
        }
    }];
}

RCT_EXPORT_METHOD(loadMessages:(RCTResponseSenderBlock)success fail:(RCTResponseSenderBlock)fail) {
    [PWInbox loadMessagesWithCompletion:^(NSArray<NSObject<PWInboxMessageProtocol> *> *messages, NSError *error) {
        if (success) {
            NSMutableArray* array = [[NSMutableArray alloc] init];
                for (NSObject<PWInboxMessageProtocol>* message in messages) {
                    NSDictionary* dict = [self inboxMessageToDictionary:message];
                    [array addObject:dict];
                }
            success( @[ array ]);
        } else if (error != nil && fail != nil) {
            fail(@[ error ]);
        }
    }];
}

RCT_EXPORT_METHOD(readMessage:(NSString*)code) {
    NSArray* arr = [NSArray arrayWithObject:code];
    [PWInbox readMessagesWithCodes:arr];
}

RCT_EXPORT_METHOD(readMessages:(NSArray<NSString*>*)codes) {
    [PWInbox readMessagesWithCodes:codes];
}

RCT_EXPORT_METHOD(deleteMessage:(NSString*)code) {
    NSArray* arr = [NSArray arrayWithObject:code];
    [PWInbox deleteMessagesWithCodes:arr];
}

RCT_EXPORT_METHOD(deleteMessages:(NSArray<NSString*>*)codes) {
    [PWInbox deleteMessagesWithCodes:codes];
}

RCT_EXPORT_METHOD(performAction:(NSString*)code) {
    [PWInbox performActionForMessageWithCode:code];
}

#pragma mark -
#pragma mark - Swizzling

+ (void)swizzleNotificationSettingsHandler {
    if ([UIApplication sharedApplication].delegate == nil) {
        return;
    }
    
    if ([[[UIDevice currentDevice] systemVersion] floatValue] < 8.0) {
        return;
    }
    
    static Class appDelegateClass = nil;
    
    //do not swizzle the same class twice
    id delegate = [UIApplication sharedApplication].delegate;
    if(appDelegateClass == [delegate class]) {
        return;
    }
    
    appDelegateClass = [delegate class];
    
    pushwoosh_swizzle([delegate class], @selector(application:didRegisterForRemoteNotificationsWithDeviceToken:), @selector(application:pwplugin_didRegisterWithDeviceToken:), (IMP)pwplugin_didRegisterWithDeviceToken, "v@:::");
    pushwoosh_swizzle([delegate class], @selector(application:didFailToRegisterForRemoteNotificationsWithError:), @selector(application:pwplugin_didFailToRegisterForRemoteNotificationsWithError:), (IMP)pwplugin_didFailToRegisterForRemoteNotificationsWithError, "v@:::");
    pushwoosh_swizzle([delegate class], @selector(application:didReceiveRemoteNotification:fetchCompletionHandler:), @selector(application:pwplugin_didReceiveRemoteNotification:fetchCompletionHandler:), (IMP)pwplugin_didReceiveRemoteNotification, "v@::::");
}

void pwplugin_didReceiveRemoteNotification(id self, SEL _cmd, UIApplication * application, NSDictionary * userInfo, void (^completionHandler)(UIBackgroundFetchResult)) {
    if ([self respondsToSelector:@selector(application:pwplugin_didReceiveRemoteNotification:fetchCompletionHandler:)]) {
        [self application:application pwplugin_didReceiveRemoteNotification:userInfo fetchCompletionHandler:completionHandler];
    }
    
    [[Pushwoosh sharedInstance] handlePushReceived:userInfo];
}

void pwplugin_didRegisterWithDeviceToken(id self, SEL _cmd, id application, NSData *deviceToken) {
    if ([self respondsToSelector:@selector(application: pwplugin_didRegisterWithDeviceToken:)]) {
        [self application:application pwplugin_didRegisterWithDeviceToken:deviceToken];
    }
    
    [[Pushwoosh sharedInstance] handlePushRegistration:deviceToken];
}

void pwplugin_didFailToRegisterForRemoteNotificationsWithError(id self, SEL _cmd, UIApplication *application, NSError *error) {
    if ([self respondsToSelector:@selector(application:pwplugin_didFailToRegisterForRemoteNotificationsWithError:)]) {
        [self application:application pwplugin_didFailToRegisterForRemoteNotificationsWithError:error];
    }
    
    [[Pushwoosh sharedInstance] handlePushRegistrationFailure:error];
}

#pragma mark - UNUserNotificationCenter Delegate Methods
#pragma mark -

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:
(void (^)(UNNotificationPresentationOptions options))completionHandler
API_AVAILABLE(ios(10.0)) {
    
    if ([self isRemoteNotification:notification] && [PWMessage isPushwooshMessage:notification.request.content.userInfo]) {
        completionHandler(UNNotificationPresentationOptionNone);
    } else if ([PushNotificationManager pushManager].showPushnotificationAlert || [notification.request.content.userInfo objectForKey:@"pw_push"] == nil) {
        completionHandler(UNNotificationPresentationOptionBadge | UNNotificationPresentationOptionAlert | UNNotificationPresentationOptionSound);
    } else {
        completionHandler(UNNotificationPresentationOptionNone);
    }
    
    if (_originalNotificationCenterDelegate != nil &&
        _originalNotificationCenterDelegateResponds.willPresentNotification) {
        [_originalNotificationCenterDelegate userNotificationCenter:center
                                            willPresentNotification:notification
                                              withCompletionHandler:completionHandler];
    }
}

- (BOOL)isContentAvailablePush:(NSDictionary *)userInfo {
    NSDictionary *apsDict = userInfo[@"aps"];
    return apsDict[@"content-available"] != nil;
}

- (NSDictionary *)pushPayloadFromContent:(UNNotificationContent *)content {
    return [[content.userInfo objectForKey:@"pw_push"] isKindOfClass:[NSDictionary class]] ? [content.userInfo objectForKey:@"pw_push"] : content.userInfo;
}

- (BOOL)isRemoteNotification:(UNNotification *)notification {
    return [notification.request.trigger isKindOfClass:[UNPushNotificationTrigger class]];
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
         withCompletionHandler:(void (^)(void))completionHandler
API_AVAILABLE(ios(10.0)) {
    dispatch_block_t handlePushAcceptanceBlock = ^{
        if (![response.actionIdentifier isEqualToString:UNNotificationDismissActionIdentifier]) {
            if (![response.actionIdentifier isEqualToString:UNNotificationDefaultActionIdentifier] && [[PushNotificationManager pushManager].delegate respondsToSelector:@selector(onActionIdentifierReceived:withNotification:)]) {
                [[PushNotificationManager pushManager].delegate onActionIdentifierReceived:response.actionIdentifier withNotification:[self pushPayloadFromContent:response.notification.request.content]];
            }
        }
    };
    
    if ([self isRemoteNotification:response.notification]  && [PWMessage isPushwooshMessage:response.notification.request.content.userInfo]) {
        handlePushAcceptanceBlock();
    } else if ([response.notification.request.content.userInfo objectForKey:@"pw_push"]) {
        handlePushAcceptanceBlock();
    }

    if (_originalNotificationCenterDelegate != nil &&
        _originalNotificationCenterDelegateResponds.didReceiveNotificationResponse) {
        [_originalNotificationCenterDelegate userNotificationCenter:center
                                     didReceiveNotificationResponse:response
                                              withCompletionHandler:completionHandler];
    } else {
        completionHandler();
    }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
   openSettingsForNotification:(nullable UNNotification *)notification
API_AVAILABLE(ios(10.0)) {
    if ([[PushNotificationManager pushManager].delegate respondsToSelector:@selector(pushManager:openSettingsForNotification:)]) {
        #pragma clang diagnostic push
        #pragma clang diagnostic ignored "-Wpartial-availability"
        [[PushNotificationManager pushManager].delegate pushManager:[PushNotificationManager pushManager] openSettingsForNotification:notification];
        #pragma clang diagnostic pop
    }

    if (_originalNotificationCenterDelegate != nil &&
        _originalNotificationCenterDelegateResponds.openSettingsForNotification) {
#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Wunguarded-availability-new"
        [_originalNotificationCenterDelegate userNotificationCenter:center
                                        openSettingsForNotification:notification];
#pragma clang diagnostic pop
    }
}

- (NSDictionary*)inboxMessageToDictionary:(NSObject<PWInboxMessageProtocol>*) message {
    NSMutableDictionary* dictionary = [[NSMutableDictionary alloc] init];
    [dictionary setValue:@(message.type) forKey:@"type"];
    [dictionary setValue:[self stringOrEmpty: message.imageUrl] forKey:@"imageUrl"];
    [dictionary setValue:[self stringOrEmpty: message.code] forKey:@"code"];
    [dictionary setValue:[self stringOrEmpty: message.title] forKey:@"title"];
    [dictionary setValue:[self stringOrEmpty: message.message] forKey:@"message"];
    [dictionary setValue:[self stringOrEmpty: [self dateToString:message.sendDate]] forKey:@"sendDate"];
    [dictionary setValue:@(message.isRead) forKey:@"isRead"];
    [dictionary setValue:@(message.isActionPerformed) forKey:@"isActionPerformed"];
    
    NSDictionary* actionParams = [NSDictionary dictionaryWithDictionary:message.actionParams];
    NSData* customData = [actionParams valueForKey:@"u"];
    [dictionary setValue:customData forKey:@"customData"];
    
    NSDictionary* result = [NSDictionary dictionaryWithDictionary:dictionary];
    return result;
}

- (NSString *)stringOrEmpty:(NSString *)string {
    return string != nil ? string : @"";
}

- (NSString*)dateToString:(NSDate*)date {
    NSDateFormatter *formatter = [[NSDateFormatter alloc] init];
    [formatter setDateFormat:@"yyyy-MM-dd'T'H:mm:ssZ"];
    return [formatter stringFromDate:date];
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
    UIViewController *topViewController = [PushwooshPlugin findRootViewController];
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

RCT_EXPORT_METHOD(clearNotificationCenter){
    [PushNotificationManager clearNotificationCenter];
}


RCT_EXPORT_METHOD(enableHuaweiPushNotifications) {
    // available in Android only
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
//    [self sendEventWithName:event body:args];
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
