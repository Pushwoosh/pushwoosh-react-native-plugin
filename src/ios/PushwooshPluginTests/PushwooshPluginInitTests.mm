//
//  PushwooshPluginInitTests.mm
//  PushwooshPluginTests
//
//  init() and the push that started the app.
//
//  On a cold start the SDK reports the tapped push before the JS bundle has subscribed to
//  anything. The plugin has to hold it, hand it to JS once init() runs (as pushReceived and
//  pushOpened) and expose its deep link through Linking.getInitialURL(), which the New
//  Architecture reads from the swizzled RCTLinkingManager.
//

#import "PWPluginTestCase.h"

// The release gate rejects anything shaped like a real application code (XXXXX-XXXXX).
static NSString *const PWTestAppCode = @"XXXXX-XXXXX";

@interface PushwooshPluginInitTests : PWPluginTestCase
@end

@implementation PushwooshPluginInitTests

- (NSDictionary *)launchPushWithLink:(NSString *)link {
    NSMutableDictionary *push = [@{ @"aps" : @{ @"alert" : @"Launch" }, @"pw_msg" : @1, @"p" : [NSUUID UUID].UUIDString } mutableCopy];
    if (link) {
        push[@"l"] = link;
    }
    return push;
}

// Verifies that init() without pw_appid reports the error to JS and leaves the SDK untouched.
- (void)testInitWithoutAppCodeReportsErrorAndLeavesSdkUntouched {
    OCMReject(ClassMethod([self.pushManager initializeWithAppCode:[OCMArg any] appName:[OCMArg any]]));
    NSMutableArray *successes = [NSMutableArray array];
    NSMutableArray *errors = [NSMutableArray array];

    [self.plugin init:@{ @"pw_notification_handling" : @"CUSTOM" } success:^(NSArray *response) {
        [successes addObject:response];
    } error:^(NSArray *response) {
        [errors addObject:response];
    }];

    XCTAssertEqualObjects(errors, @[ @[ @"pw_appid is missing" ] ]);
    XCTAssertEqualObjects(successes, @[]);
}

// Verifies that init() starts the SDK with the app code, takes the push delegate and confirms to JS.
- (void)testInitStartsSdkAndConfirms {
    [self stubSDKInitialization];
    NSMutableArray *successes = [NSMutableArray array];
    NSMutableArray *errors = [NSMutableArray array];

    [self.plugin init:@{ @"pw_appid" : PWTestAppCode } success:^(NSArray *response) {
        [successes addObject:response];
    } error:^(NSArray *response) {
        [errors addObject:response];
    }];

    OCMVerify(ClassMethod([self.pushManager initializeWithAppCode:PWTestAppCode appName:nil]));
    OCMVerify([self.pushManager sendAppOpen]);
    OCMVerify([(PushNotificationManager *)self.pushManager setDelegate:self.plugin]);
    XCTAssertEqualObjects(successes, @[ @[] ]);
    XCTAssertEqualObjects(errors, @[]);
}

// Verifies that init() emits no device event when no push started the app.
- (void)testInitEmitsNothingWithoutLaunchPush {
    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects(self.jsEvents, @[]);
}

// Verifies that the push that started the app is handed to JS on init() as both pushReceived
// and pushOpened, so the app can route it.
- (void)testInitReplaysLaunchPushToJs {
    NSDictionary *push = [self launchPushWithLink:nil];
    [self simulateLaunchPush:push];

    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushReceived"], @[ push ]);
    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushOpened"], @[ push ]);
}

// Verifies that when the plugin saw no early push, the SDK's own launch notification is replayed.
- (void)testInitReplaysSdkLaunchNotificationWhenNoEarlyPushWasSeen {
    NSDictionary *push = [self launchPushWithLink:nil];
    OCMStub([self.pushManager launchNotification]).andReturn(push);

    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushReceived"], @[ push ]);
    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushOpened"], @[ push ]);
}

// Verifies that a second init() does not replay the launch push a second time. Neither source of
// the launch push is cleared by init(), so an app that initialises twice - a consent flow, a
// switch of application code, a Fast Refresh reload - used to see pushReceived and pushOpened
// again for a push it had already handled.
- (void)testSecondInitDoesNotReplayTheLaunchPush {
    NSDictionary *push = [self launchPushWithLink:nil];
    [self simulateLaunchPush:push];

    [self initPluginWithAppCode:PWTestAppCode];
    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushReceived"], @[ push ]);
    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushOpened"], @[ push ]);
}

// Verifies the same for the SDK's own launch notification, which the plugin does not own and
// cannot clear.
- (void)testSecondInitDoesNotReplaySdkLaunchNotification {
    NSDictionary *push = [self launchPushWithLink:nil];
    OCMStub([self.pushManager launchNotification]).andReturn(push);

    [self initPluginWithAppCode:PWTestAppCode];
    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"pushOpened"], @[ push ]);
}

// Verifies that a second init() does not re-arm the deep link Linking.getInitialURL() already
// consumed: the user would be routed back to the screen the push opened on the cold start.
- (void)testSecondInitDoesNotReArmAConsumedDeepLink {
    [self simulateLaunchPush:[self launchPushWithLink:@"pwdemo://inbox"]];
    [self initPluginWithAppCode:PWTestAppCode];
    XCTAssertEqualObjects([self initialURLFromLinkingManager], @"pwdemo://inbox");

    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that the launch push's app-scheme link is what Linking.getInitialURL() resolves to
// after init(), and only once.
- (void)testInitExposesLaunchPushDeepLinkThroughGetInitialURL {
    [self simulateLaunchPush:[self launchPushWithLink:@"pwdemo://inbox"]];

    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], @"pwdemo://inbox");
    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that an http(s) link stays with the native SDK (Universal Links or the browser) and
// never shows up as the app's initial URL.
- (void)testInitKeepsWebLinksAwayFromGetInitialURL {
    [self simulateLaunchPush:[self launchPushWithLink:@"https://example.com/promo"]];

    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that the legacy onPushOpen() callback receives the launch push right away and that
// the push is then spent: a second subscriber gets nothing until a new push arrives.
- (void)testOnPushOpenDeliversLaunchPushOnce {
    NSDictionary *push = [self launchPushWithLink:nil];
    [self simulateLaunchPush:push];
    NSMutableArray *first = [NSMutableArray array];
    NSMutableArray *second = [NSMutableArray array];

    [self.plugin onPushOpen:^(NSArray *response) {
        [first addObject:response];
    }];
    [self.plugin onPushOpen:^(NSArray *response) {
        [second addObject:response];
    }];

    XCTAssertEqualObjects(first, @[ @[ push ] ]);
    XCTAssertEqualObjects(second, @[]);
}

// Verifies that a push in the launch options is accepted through the SDK once: the
// PLUGIN_NOTIFICATION_HANDLER mode suppresses the SDK's own delegate, and the plugin's is
// installed only in init(), too late for a cold start.
- (void)testColdStartPushFromLaunchOptionsIsAcceptedOnce {
    NSDictionary *push = [self launchPushWithLink:nil];
    __block NSUInteger accepted = 0;
    OCMStub([self.pushManager handlePushAccepted:push onStart:YES]).andDo(^(NSInvocation *invocation) {
        accepted++;
    });

    [[NSNotificationCenter defaultCenter] postNotificationName:UIApplicationDidFinishLaunchingNotification
                                                        object:nil
                                                      userInfo:@{ UIApplicationLaunchOptionsRemoteNotificationKey : push }];
    [[NSNotificationCenter defaultCenter] postNotificationName:UIApplicationDidFinishLaunchingNotification
                                                        object:nil
                                                      userInfo:@{ UIApplicationLaunchOptionsRemoteNotificationKey : push }];

    XCTAssertEqual(accepted, 1);
}

// Verifies that a deep link in the launch options is routed to Linking.getInitialURL() instead
// of through the SDK, which would deliver it a second time.
- (void)testColdStartDeepLinkFromLaunchOptionsReachesGetInitialURL {
    NSDictionary *push = [self launchPushWithLink:@"pwdemo://orders/42"];
    OCMReject([self.pushManager handlePushAccepted:[OCMArg any] onStart:YES]);

    [[NSNotificationCenter defaultCenter] postNotificationName:UIApplicationDidFinishLaunchingNotification
                                                        object:nil
                                                      userInfo:@{ UIApplicationLaunchOptionsRemoteNotificationKey : push }];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], @"pwdemo://orders/42");
}

@end
