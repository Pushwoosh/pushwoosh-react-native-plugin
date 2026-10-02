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
#import <React/RCTLinkingManager.h>

// The release gate rejects anything shaped like a real application code (XXXXX-XXXXX).
static NSString *const PWTestAppCode = @"XXXXX-XXXXX";

// A Universal Link as the SDK would deliver it: an http(s) `l` of the launch push.
static NSString *const PWTestWebLink = @"https://example.com/promo";

@interface PushwooshPluginInitTests : PWPluginTestCase
@end

@implementation PushwooshPluginInitTests

- (UNNotificationResponse *)responseTappingPush:(NSDictionary *)push {
    id content = OCMClassMock([UNNotificationContent class]);
    OCMStub([content userInfo]).andReturn(push);
    id request = OCMClassMock([UNNotificationRequest class]);
    OCMStub([request content]).andReturn(content);
    OCMStub([request trigger]).andReturn(OCMClassMock([UNPushNotificationTrigger class]));
    id notification = OCMClassMock([UNNotification class]);
    OCMStub([notification request]).andReturn(request);
    id response = OCMClassMock([UNNotificationResponse class]);
    OCMStub([response notification]).andReturn(notification);
    OCMStub([response actionIdentifier]).andReturn(UNNotificationDefaultActionIdentifier);
    return response;
}

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

// Verifies that an http(s) link stays with the native SDK (Universal Links or the browser): init()
// does not hand it to getInitialURL(), which waits for the SDK's delivery window and then answers null.
- (void)testInitKeepsWebLinksAwayFromGetInitialURL {
    [self simulateLaunchPush:[self launchPushWithLink:@"https://example.com/promo"]];
    [self initPluginWithAppCode:PWTestAppCode];

    NSMutableArray *initialURL = [self requestInitialURL];
    XCTAssertEqualObjects(initialURL, @[]);
    [self waitForInitialURL:initialURL];

    XCTAssertEqualObjects(initialURL, @[ [NSNull null] ]);
}

// Verifies the usual cold-start race: JS asks Linking.getInitialURL() just before the SDK delivers
// the link, and every call held meanwhile - React Navigation's and the app's own - gets it.
- (void)testGetInitialURLCalledBeforeDeliveryResolvesEveryHeldCallWithTheLaunchLink {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];

    NSMutableArray *first = [self requestInitialURL];
    NSMutableArray *second = [self requestInitialURL];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects(first, @[ PWTestWebLink ]);
    XCTAssertEqualObjects(second, @[ PWTestWebLink ]);
    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that a getInitialURL() held for the launch push's Universal Link waits for it through the
// delivery window: the SDK may hand the link over seconds after the tap - it checks the host's
// apple-app-site-association over the network first - and another URL opened meanwhile does not
// answer the call.
- (void)testGetInitialURLHeldForALinkDeliveredSecondsAfterTheTapResolvesWithIt {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];
    [self waitSeconds:1];
    NSMutableArray *initialURL = [self requestInitialURL];

    [self waitSeconds:4];
    [self postOpenURLNotification:@"https://example.com/other"];
    XCTAssertEqualObjects(initialURL, @[]);
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects(initialURL, @[ PWTestWebLink ]);
}

// Verifies that the delivery window runs from the tap: the SDK accepting the same launch push again
// (at init(), when the plugin does not own notifications) does not extend it, so a getInitialURL()
// held for a link that never comes answers by the end of the tap's window.
- (void)testSameLaunchPushArmedAgainDoesNotExtendTheDeliveryWindow {
    NSDictionary *push = [self launchPushWithLink:PWTestWebLink];
    [self simulateLaunchPush:push];
    [self waitSeconds:3];
    [self simulateLaunchPush:push];
    [self initPluginWithAppCode:PWTestAppCode];
    NSMutableArray *initialURL = [self requestInitialURL];

    [self waitSeconds:2];
    XCTAssertEqualObjects(initialURL, @[]);
    [self waitSeconds:1.5];

    XCTAssertEqualObjects(initialURL, @[ [NSNull null] ]);
}

// Verifies that the SDK accepting the same launch push again after the tap's delivery window ended -
// JS called init() that late - does not open the window again: nothing would end it, so a later
// getInitialURL() would be held for good.
- (void)testSameLaunchPushArmedAgainAfterTheDeliveryWindowDoesNotHoldGetInitialURL {
    NSDictionary *push = [self launchPushWithLink:PWTestWebLink];
    [self simulateLaunchPush:push];
    [self waitSeconds:6.5];
    [self simulateLaunchPush:push];
    [self initPluginWithAppCode:PWTestAppCode];

    XCTAssertEqualObjects([self requestInitialURL], @[ [NSNull null] ]);
}

// Verifies that a later launch push without a web link releases a held getInitialURL() at once.
- (void)testLaterLaunchPushWithoutWebLinkReleasesAHeldGetInitialURL {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];
    NSMutableArray *initialURL = [self requestInitialURL];

    [self simulateLaunchPush:[self launchPushWithLink:nil]];

    XCTAssertEqualObjects(initialURL, @[ [NSNull null] ]);
}

// Verifies that the launch push's Universal Link, handed to RCTLinkingManager by the SDK before
// JS asks for it, is what a later Linking.getInitialURL() resolves to, and only once.
- (void)testWebLaunchLinkReachesGetInitialURLWhenNobodyListensForURLEvents {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], PWTestWebLink);
    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that only the launch push's own link is caught: another URL opened meanwhile passes by
// untouched, and the launch link is still expected afterwards.
- (void)testOnlyTheLaunchPushWebLinkIsCaughtAndItSurvivesOtherURLs {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];

    [self postOpenURLNotification:@"https://example.com/other"];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], PWTestWebLink);
}

// Verifies that a link delivered after a held getInitialURL() gave up goes to the `url` event only,
// so a later getInitialURL() (JS reload, OTA update) cannot route to an old push's screen.
- (void)testLinkDeliveredAfterGetInitialURLAnsweredGoesToTheURLEventOnly {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];
    [self linkingManagerListeningForURLEvents];
    NSMutableArray *initialURL = [self requestInitialURL];
    [self waitForInitialURL:initialURL];
    XCTAssertEqualObjects(initialURL, @[ [NSNull null] ]);

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"url"], @[ @{ @"url" : PWTestWebLink } ]);
    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that the SDK accepting the same launch push again - it does so at init() when the plugin
// does not own notifications - after JS has read getInitialURL() does not make a late link parkable.
- (void)testSameLaunchPushArmedAgainAfterGetInitialURLAnsweredDoesNotParkTheLateLink {
    NSDictionary *push = [self launchPushWithLink:PWTestWebLink];
    [self simulateLaunchPush:push];
    [self initPluginWithAppCode:PWTestAppCode];
    NSMutableArray *initialURL = [self requestInitialURL];
    [self waitForInitialURL:initialURL];
    XCTAssertEqualObjects(initialURL, @[ [NSNull null] ]);

    [self simulateLaunchPush:push];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that the launch push's link, once it answered a held getInitialURL(), is not parked when
// the SDK accepts the same push again within the window and delivers the link a second time: a later
// getInitialURL() (JS reload, OTA update) would route to the old push's screen.
- (void)testLinkDeliveredAgainAfterItAnsweredAHeldGetInitialURLIsNotParked {
    NSDictionary *push = [self launchPushWithLink:PWTestWebLink];
    [self simulateLaunchPush:push];
    [self initPluginWithAppCode:PWTestAppCode];
    NSMutableArray *initialURL = [self requestInitialURL];
    [self postOpenURLNotification:PWTestWebLink];
    XCTAssertEqualObjects(initialURL, @[ PWTestWebLink ]);

    [self simulateLaunchPush:push];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that the link stays armed only for the SDK's delivery window: the same URL opened later
// (from Safari, say) is not taken for the launch push's link.
- (void)testLinkDeliveredAfterTheDeliveryWindowIsNotCaught {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];

    XCTestExpectation *windowPassed = [self expectationWithDescription:@"delivery window passed"];
    dispatch_after(dispatch_time(DISPATCH_TIME_NOW, (int64_t)(6.5 * NSEC_PER_SEC)), dispatch_get_main_queue(), ^{
        [windowPassed fulfill];
    });
    [self waitForExpectations:@[ windowPassed ] timeout:10];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that a Universal Link opened without a launch push (the user tapped a link in Safari
// while the app was closed) is not mistaken for a push link.
- (void)testURLWithoutLaunchPushStaysOutOfGetInitialURL {
    [self initPluginWithAppCode:PWTestAppCode];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that a second init() does not re-arm a web link Linking.getInitialURL() already
// consumed, and that the same URL delivered again is no longer caught.
- (void)testSecondInitDoesNotReArmAConsumedWebLaunchLink {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];
    [self postOpenURLNotification:PWTestWebLink];
    XCTAssertEqualObjects([self initialURLFromLinkingManager], PWTestWebLink);

    [self initPluginWithAppCode:PWTestAppCode];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that a launch push read from the launch options arms its web link too, before the SDK
// (stubbed here) accepts it.
- (void)testWebLaunchLinkFromLaunchOptionsReachesGetInitialURL {
    NSDictionary *push = [self launchPushWithLink:PWTestWebLink];
    [[NSNotificationCenter defaultCenter] postNotificationName:UIApplicationDidFinishLaunchingNotification
                                                        object:nil
                                                      userInfo:@{ UIApplicationLaunchOptionsRemoteNotificationKey : push }];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], PWTestWebLink);
}

// Verifies that a notification some other code posts under RN's name, without a string `url`,
// neither crashes the plugin nor spends the pending link.
- (void)testForeignOpenURLNotificationWithoutURLIsIgnored {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];

    [[NSNotificationCenter defaultCenter] postNotificationName:@"RCTOpenURLNotification" object:nil userInfo:nil];
    [[NSNotificationCenter defaultCenter] postNotificationName:@"RCTOpenURLNotification" object:nil userInfo:@{ @"url" : @42 }];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], PWTestWebLink);
}

// Verifies that a later launch push without a web link drops the earlier pending one: a link
// from a push the user opened minutes ago must not be caught on this start.
- (void)testLaterLaunchPushWithoutWebLinkDropsThePendingOne {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self simulateLaunchPush:[self launchPushWithLink:nil]];
    [self initPluginWithAppCode:PWTestAppCode];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that the scheme check is case-insensitive, like the app-scheme one: an `HTTPS://` link
// must not fall between the two channels.
- (void)testWebLaunchLinkSchemeIsMatchedCaseInsensitively {
    NSString *link = @"HTTPS://example.com/promo";
    [self simulateLaunchPush:[self launchPushWithLink:link]];
    [self initPluginWithAppCode:PWTestAppCode];

    [self postOpenURLNotification:link];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], link);
}

// Verifies that a JS `url` listener (React Navigation subscribes on mount) does not keep the launch
// link out of getInitialURL(): it reaches the listener and is parked once.
- (void)testWebLaunchLinkIsParkedForGetInitialURLEvenWhenJSAlreadyListens {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];
    [self linkingManagerListeningForURLEvents];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"url"], @[ @{ @"url" : PWTestWebLink } ]);
    XCTAssertEqualObjects([self initialURLFromLinkingManager], PWTestWebLink);
    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that with the JS `url` listener removed again RN delivers no event and the launch link
// is still what getInitialURL() resolves to.
- (void)testWebLaunchLinkReachesGetInitialURLAfterJSRemovedItsURLListener {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];
    [self initPluginWithAppCode:PWTestAppCode];
    RCTLinkingManager *linkingManager = [self linkingManagerListeningForURLEvents];
    [linkingManager removeListeners:1];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self jsEventBodiesNamed:@"url"], @[]);
    XCTAssertEqualObjects([self initialURLFromLinkingManager], PWTestWebLink);
}

// Verifies that the link is matched in the URL.absoluteString form RCTLinkingManager posts, not
// the raw push link with a non-ASCII character.
- (void)testWebLaunchLinkWithNonCanonicalCharactersIsCanonicalizedBeforeMatching {
    if (@available(iOS 17.0, *)) {
    } else {
        XCTSkip(@"NSURL percent-encodes a non-ASCII character only from iOS 17");
    }
    NSString *rawLink = @"https://example.com/promo?q=é";
    [self simulateLaunchPush:[self launchPushWithLink:rawLink]];
    [self initPluginWithAppCode:PWTestAppCode];

    [self postOpenURLNotification:rawLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], @"https://example.com/promo?q=%C3%A9");
    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that the SDK's onStart:NO callback for a push opened while the app runs never arms the
// pending web link: only a launch push can be caught on its way to RCTLinkingManager.
- (void)testPushWithoutOnStartDoesNotArmAPendingWebLink {
    [[UIApplication sharedApplication] onPushAccepted:nil withNotification:[self launchPushWithLink:PWTestWebLink] onStart:NO];
    [self initPluginWithAppCode:PWTestAppCode];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

// Verifies that after init() the early cold-start delegate leaves a tapped push to the delegate
// that owns notifications: it neither accepts the push a second time nor arms its web link.
- (void)testEarlyDelegateLeavesATapAfterInitToTheOwningDelegate {
    [self initPluginWithAppCode:PWTestAppCode];
    OCMReject([self.pushManager handlePushAccepted:[OCMArg any] onStart:YES]);
    id<UNUserNotificationCenterDelegate> earlyDelegate = [NSClassFromString(@"PWEarlyNotificationDelegate") new];
    __block BOOL completed = NO;

    [earlyDelegate userNotificationCenter:[UNUserNotificationCenter currentNotificationCenter]
           didReceiveNotificationResponse:[self responseTappingPush:[self launchPushWithLink:PWTestWebLink]]
                    withCompletionHandler:^{
        completed = YES;
    }];
    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertTrue(completed);
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

// Verifies that a later app-scheme launch push from the launch options drops an earlier pending
// web link, so a push opened minutes ago cannot resurface on this start.
- (void)testAppSchemeLaunchPushFromLaunchOptionsDropsAnEarlierPendingWebLink {
    [self simulateLaunchPush:[self launchPushWithLink:PWTestWebLink]];

    NSDictionary *push = [self launchPushWithLink:@"pwdemo://orders/42"];
    OCMReject([self.pushManager handlePushAccepted:[OCMArg any] onStart:YES]);
    [[NSNotificationCenter defaultCenter] postNotificationName:UIApplicationDidFinishLaunchingNotification
                                                        object:nil
                                                      userInfo:@{ UIApplicationLaunchOptionsRemoteNotificationKey : push }];

    [self postOpenURLNotification:PWTestWebLink];

    XCTAssertEqualObjects([self initialURLFromLinkingManager], @"pwdemo://orders/42");
    XCTAssertEqualObjects([self initialURLFromLinkingManager], [NSNull null]);
}

@end
