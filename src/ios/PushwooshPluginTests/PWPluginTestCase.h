//
//  PWPluginTestCase.h
//  PushwooshPluginTests
//
//  Base class of the plugin's unit tests: the module under test, a mocked SDK and a recorder
//  for the events the module sends to JS.
//

#import <XCTest/XCTest.h>
#import <OCMock/OCMock.h>
#import <UserNotifications/UserNotifications.h>

#import <React/RCTBridgeModule.h>
#import <pushwoosh-react-native-plugin/Pushwoosh.h>

NS_ASSUME_NONNULL_BEGIN

// RCT_EXPORT_METHOD declares the bridge methods in Pushwoosh.mm only. The tests call them the way
// the bridge would, so the selectors are spelled out here.
@interface PushwooshPlugin (PWPluginTestSelectors)

- (void)init:(NSDictionary *)config success:(nullable RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)registerForPushNotifications:(nullable RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)onPushOpen:(RCTResponseSenderBlock)callback;
- (void)onPushReceived:(RCTResponseSenderBlock)callback;
- (void)setEmails:(NSArray *)emails success:(nullable RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)setUserEmails:(NSString *)userId emails:(NSArray *)emails success:(nullable RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)setTags:(NSDictionary *)tags success:(nullable RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)getTags:(nullable RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)setUserId:(NSString *)userId success:(nullable RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)loadMessages:(nullable RCTResponseSenderBlock)success fail:(nullable RCTResponseSenderBlock)fail;
- (void)setRichMediaType:(double)type;
- (void)getRichMediaType:(RCTResponseSenderBlock)callback;
- (void)isCommunicationEnabled:(RCTResponseSenderBlock)callback;
- (void)setCommunicationEnabled:(BOOL)enabled success:(RCTResponseSenderBlock)success error:(nullable RCTResponseSenderBlock)error;
- (void)createLocalNotification:(NSDictionary *)params;

@end

// Pushwoosh.mm keeps the launch push in a UIApplication category that the SDK calls before the
// module exists; a test drives a cold start through it.
@interface UIApplication (PWPluginTestSelectors)

- (void)onPushAccepted:(nullable PushNotificationManager *)manager withNotification:(NSDictionary *)pushNotification onStart:(BOOL)onStart;

@end

@interface PWPluginTestCase : XCTestCase

/// The module under test, with JS events routed into `jsEvents`.
@property (nonatomic, strong, readonly) PushwooshPlugin *plugin;
/// OCMock behind `[PushNotificationManager pushManager]`; also carries the class-method stubs.
@property (nonatomic, strong, readonly) id pushManager;
/// OCMock behind `[Pushwoosh sharedInstance]`; also carries the class-method stubs.
@property (nonatomic, strong, readonly) id pushwoosh;
/// Every RCTDeviceEventEmitter.emit the module sent, as `@{ @"name" : ..., @"body" : ... }`.
@property (nonatomic, strong, readonly) NSMutableArray<NSDictionary *> *jsEvents;

/// The bodies of the JS events named `name`, in the order they were sent.
- (NSArray *)jsEventBodiesNamed:(NSString *)name;

/// Keeps `+[PushNotificationManager initializeWithAppCode:appName:]` away from the real SDK.
- (void)stubSDKInitialization;

/// init() as JS calls it on startup, with the SDK initialization stubbed.
- (void)initPluginWithAppCode:(NSString *)appCode;

/// A push tapped before React Native started: the SDK reports it to the UIApplication category.
- (void)simulateLaunchPush:(NSDictionary *)push;

/// What `Linking.getInitialURL()` would resolve to now: the URL string, or NSNull when nothing is pending.
- (id)initialURLFromLinkingManager;

@end

NS_ASSUME_NONNULL_END
