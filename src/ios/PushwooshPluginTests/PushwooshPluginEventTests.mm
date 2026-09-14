//
//  PushwooshPluginEventTests.mm
//  PushwooshPluginTests
//
//  How SDK callbacks turn into the JS device events and the legacy callbacks.
//

#import "PWPluginTestCase.h"

#import <pushwoosh-react-native-plugin/PWEventDispatcher.h>

/// The notification centre delegate the app had before the plugin took over, counting what the
/// plugin forwards to it.
@interface PWRecordingNotificationDelegate : NSObject <UNUserNotificationCenterDelegate>
@property (nonatomic, assign) NSUInteger willPresentCalls;
@end

@implementation PWRecordingNotificationDelegate

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler {
    self.willPresentCalls++;
    completionHandler(UNNotificationPresentationOptionNone);
}

@end

@interface PushwooshPluginEventTests : PWPluginTestCase
@end

@implementation PushwooshPluginEventTests

- (NSDictionary *)push:(NSString *)title {
    return @{ @"aps" : @{ @"alert" : title }, @"pw_msg" : @1, @"p" : [NSUUID UUID].UUIDString };
}

- (id)mockNotificationWithUserInfo:(NSDictionary *)userInfo {
    UNMutableNotificationContent *content = [UNMutableNotificationContent new];
    content.userInfo = userInfo;
    id request = OCMClassMock([UNNotificationRequest class]);
    [self addTeardownBlock:^{ [request stopMocking]; }];
    OCMStub([request content]).andReturn(content);
    id notification = OCMClassMock([UNNotification class]);
    [self addTeardownBlock:^{ [notification stopMocking]; }];
    OCMStub([notification request]).andReturn(request);
    return notification;
}

// Verifies that the module declares exactly the events JS subscribes to.
- (void)testSupportedEventsArePushOpenedAndPushReceived {
    NSArray *events = [self.plugin supportedEvents];

    XCTAssertEqual(events.count, 2);
    XCTAssertTrue([events containsObject:@"pushOpened"]);
    XCTAssertTrue([events containsObject:@"pushReceived"]);
}

// Verifies that a push the SDK reports as received reaches JS as pushReceived with its payload.
- (void)testPushReceivedFromSdkReachesJs {
    NSDictionary *push = [self push:@"Received"];

    [self.plugin onPushReceived:nil withNotification:push onStart:NO];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushReceived"], @[ push ]);
    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushOpened"], @[]);
}

// Verifies that a push the SDK reports as tapped reaches JS as pushOpened with its payload.
- (void)testPushAcceptedFromSdkReachesJs {
    NSDictionary *push = [self push:@"Opened"];

    [self.plugin onPushAccepted:nil withNotification:push onStart:NO];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushOpened"], @[ push ]);
    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushReceived"], @[]);
}

// Verifies that events go out although nobody called addListener: the plugin's JS side listens
// through DeviceEventEmitter, which never raises RCTEventEmitter's listener count.
- (void)testEventsGoOutWithoutNativeEventEmitterListeners {
    [self.plugin removeListeners:0];

    [self.plugin onPushReceived:nil withNotification:[self push:@"Received"] onStart:NO];

    XCTAssertEqual([self jsEventBodiesNamed:@"pushReceived"].count, 1);
}

// Verifies that the legacy onPushOpen() callback fires for a tapped push and only once: React
// Native lets a callback be invoked a single time and throws on the second call.
- (void)testOnPushOpenCallbackFiresOnceForATappedPush {
    NSMutableArray *deliveries = [NSMutableArray array];
    [self.plugin onPushOpen:^(NSArray *response) {
        [deliveries addObject:response];
    }];
    NSDictionary *first = [self push:@"First"];

    [self.plugin onPushAccepted:nil withNotification:first onStart:NO];
    [self.plugin onPushAccepted:nil withNotification:[self push:@"Second"] onStart:NO];

    XCTAssertEqualObjects(deliveries, @[ @[ first ] ]);
}

// Verifies that the token the SDK registered reaches the register() success callback and a
// registration failure reaches its error callback, each once.
- (void)testRegistrationResultReachesTheRegisterCallbacks {
    NSMutableArray *tokens = [NSMutableArray array];
    NSMutableArray *errors = [NSMutableArray array];
    [self.plugin registerForPushNotifications:^(NSArray *response) {
        [tokens addObject:response];
    } error:^(NSArray *response) {
        [errors addObject:response];
    }];

    [self.plugin onDidRegisterForRemoteNotificationsWithDeviceToken:@"apns-token"];
    [self.plugin onDidFailToRegisterForRemoteNotificationsWithError:[NSError errorWithDomain:@"test" code:1 userInfo:@{ NSLocalizedDescriptionKey : @"denied" }]];
    [self.plugin onDidRegisterForRemoteNotificationsWithDeviceToken:@"apns-token-2"];

    OCMVerify([self.pushManager registerForPushNotifications]);
    XCTAssertEqualObjects(tokens, @[ @[ @"apns-token" ] ]);
    XCTAssertEqualObjects(errors, @[ @[ @"denied" ] ]);
}

// Verifies that PWEventDispatcher invokes a subscriber once and then drops it.
- (void)testEventDispatcherInvokesEachSubscriberOnlyOnce {
    PWEventDispatcher *dispatcher = [PWEventDispatcher new];
    __block NSUInteger firstCalls = 0;
    __block NSUInteger secondCalls = 0;
    [dispatcher subscribe:^(NSArray *response) { firstCalls++; } toEvent:@"push"];
    [dispatcher subscribe:^(NSArray *response) { secondCalls++; } toEvent:@"push"];

    [dispatcher dispatchEvent:@"push" withArgs:@[]];
    [dispatcher dispatchEvent:@"push" withArgs:@[]];

    XCTAssertEqual(firstCalls, 1);
    XCTAssertEqual(secondCalls, 1);
}

// Verifies that a notification presented in the foreground reaches the delegate the app had
// before the plugin took over exactly once. It used to be forwarded twice, from two blocks under
// the same condition, and UIKit expects a presentation completion handler to run a single time.
- (void)testForegroundNotificationReachesTheAppsOwnDelegateOnce {
    PWRecordingNotificationDelegate *appDelegate = [PWRecordingNotificationDelegate new];
    UNUserNotificationCenter *centre = [UNUserNotificationCenter currentNotificationCenter];
    centre.delegate = appDelegate;
    [self initPluginWithAppCode:@"XXXXX-XXXXX"];
    __block NSUInteger completions = 0;

    // Not a Pushwoosh message (no pw_msg), so the plugin hands it over straight away instead of
    // waiting for the SDK to present its own alert.
    id notification = [self mockNotificationWithUserInfo:@{ @"aps" : @{ @"alert" : @"From another SDK" } }];

    [(id<UNUserNotificationCenterDelegate>)self.plugin userNotificationCenter:centre
                                                     willPresentNotification:notification
                                                       withCompletionHandler:^(UNNotificationPresentationOptions options) {
        completions++;
    }];

    XCTAssertEqual(appDelegate.willPresentCalls, 1);
    XCTAssertEqual(completions, 1);
}

// Verifies that a Pushwoosh push the SDK presents an alert for answers UIKit once, even though the
// app's own delegate is handed the same callback afterwards and answers too. The plugin used to
// call the completion handler itself and then pass it on, so UIKit received two presentation
// decisions for one request.
- (void)testForegroundPushwooshPushAnswersPresentationOnlyOnce {
    PWRecordingNotificationDelegate *appDelegate = [PWRecordingNotificationDelegate new];
    UNUserNotificationCenter *centre = [UNUserNotificationCenter currentNotificationCenter];
    centre.delegate = appDelegate;
    [self initPluginWithAppCode:@"XXXXX-XXXXX"];
    OCMStub([self.pushManager showPushnotificationAlert]).andReturn(YES);
    __block NSUInteger completions = 0;
    __block UNNotificationPresentationOptions answered = 0;

    id notification = [self mockNotificationWithUserInfo:[self push:@"Foreground"]];

    [(id<UNUserNotificationCenterDelegate>)self.plugin userNotificationCenter:centre
                                                     willPresentNotification:notification
                                                       withCompletionHandler:^(UNNotificationPresentationOptions options) {
        completions++;
        answered = options;
    }];

    // The SDK answers straight away; the app's delegate is handed over 0.59s later.
    XCTAssertEqual(completions, 1);
    XCTAssertEqual(answered, UNNotificationPresentationOptionSound | UNNotificationPresentationOptionBadge | UNNotificationPresentationOptionAlert);

    XCTestExpectation *handedOver = [self expectationWithDescription:@"app delegate called"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(1.0 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [handedOver fulfill];
    });
    [self waitForExpectations:@[ handedOver ] timeout:3];

    XCTAssertEqual(appDelegate.willPresentCalls, 1);
    XCTAssertEqual(completions, 1);
}

// Verifies that a notification nobody claims is still answered. With no app delegate to hand it to
// and nothing for the SDK to present - a local notification, a repeat, or a push the app disabled
// the alert for - both branches used to be skipped and UIKit was left waiting for a decision it
// never got, logging that the delegate never called the completion handler.
- (void)testUnclaimedForegroundNotificationIsStillAnswered {
    UNUserNotificationCenter *centre = [UNUserNotificationCenter currentNotificationCenter];
    centre.delegate = nil;
    [self initPluginWithAppCode:@"XXXXX-XXXXX"];
    OCMStub([self.pushManager showPushnotificationAlert]).andReturn(NO);
    __block NSUInteger completions = 0;

    id notification = [self mockNotificationWithUserInfo:[self push:@"Silent"]];

    [(id<UNUserNotificationCenterDelegate>)self.plugin userNotificationCenter:centre
                                                     willPresentNotification:notification
                                                       withCompletionHandler:^(UNNotificationPresentationOptions options) {
        completions++;
    }];

    XCTAssertEqual(completions, 1);
}

@end
