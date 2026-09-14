//
//  PushwooshPluginBridgeTests.mm
//  PushwooshPluginTests
//
//  The bridge methods: how SDK results and errors are translated into the JS callbacks, and how
//  JS arguments reach the SDK.
//

#import "PWPluginTestCase.h"

#import <React/RCTConvert.h>
#import <PushwooshInboxUI/PushwooshInboxUI.h>

typedef void (^PWCompletion)(NSError *error);

@interface PushwooshPluginBridgeTests : PWPluginTestCase
@end

@implementation PushwooshPluginBridgeTests {
    NSMutableArray *_successes;
    NSMutableArray *_errors;
    RCTResponseSenderBlock _success;
    RCTResponseSenderBlock _error;
}

- (void)setUp {
    [super setUp];
    _successes = [NSMutableArray array];
    _errors = [NSMutableArray array];
    NSMutableArray *successes = _successes;
    NSMutableArray *errors = _errors;
    _success = ^(NSArray *response) {
        [successes addObject:response];
    };
    _error = ^(NSArray *response) {
        [errors addObject:response];
    };
}

- (NSError *)errorNamed:(NSString *)description {
    return [NSError errorWithDomain:@"PushwooshPluginTests" code:1 userInfo:@{ NSLocalizedDescriptionKey : description }];
}

// Stores the completion block the plugin passes to an SDK method, so a test can complete it.
- (id)captureCompletionInto:(PWCompletion __strong *)slot {
    return [OCMArg checkWithBlock:^BOOL(id block) {
        *slot = [block copy];
        return YES;
    }];
}

#pragma mark - Emails

// Verifies that an empty list is rejected before reaching the SDK, which would otherwise drop it
// without ever calling back.
- (void)testSetEmailsRejectsEmptyList {
    OCMReject([self.pushwoosh setEmails:[OCMArg any] completion:[OCMArg any]]);

    [self.plugin setEmails:@[] success:_success error:_error];

    XCTAssertEqualObjects(_errors, @[ @[ @"emails must be a non-empty array" ] ]);
    XCTAssertEqualObjects(_successes, @[]);
}

// Verifies that success is reported once, after the SDK confirmed the last address: the SDK
// completes once per address, a React Native callback fires only once.
- (void)testSetEmailsConfirmsOnceAfterTheLastAddress {
    __block PWCompletion completion = nil;
    OCMStub(([self.pushwoosh setEmails:@[ @"one@example.com", @"two@example.com" ] completion:[self captureCompletionInto:&completion]]));

    [self.plugin setEmails:@[ @"one@example.com", @"two@example.com" ] success:_success error:_error];
    XCTAssertNotNil(completion);
    completion(nil);
    XCTAssertEqualObjects(_successes, @[]);
    completion(nil);

    XCTAssertEqualObjects(_successes, @[ @[] ]);
    XCTAssertEqualObjects(_errors, @[]);
}

// Verifies that the first failing address ends the call with its error and later completions
// change nothing.
- (void)testSetEmailsReportsTheFirstFailureOnly {
    __block PWCompletion completion = nil;
    OCMStub([self.pushwoosh setEmails:[OCMArg any] completion:[self captureCompletionInto:&completion]]);

    [self.plugin setEmails:@[ @"one@example.com", @"two@example.com" ] success:_success error:_error];
    completion([self errorNamed:@"invalid address"]);
    completion(nil);
    completion([self errorNamed:@"another"]);

    XCTAssertEqualObjects(_errors, @[ @[ @"invalid address" ] ]);
    XCTAssertEqualObjects(_successes, @[]);
}

// Verifies that setUserEmails() rejects an empty user id and an empty list before the SDK sees them.
- (void)testSetUserEmailsRejectsEmptyUserIdAndEmptyList {
    OCMReject([self.pushwoosh setUser:[OCMArg any] emails:[OCMArg any] completion:[OCMArg any]]);

    [self.plugin setUserEmails:@"" emails:@[ @"one@example.com" ] success:_success error:_error];
    [self.plugin setUserEmails:@"user-1" emails:@[] success:_success error:_error];

    XCTAssertEqualObjects(_errors, (@[ @[ @"userId must not be empty" ], @[ @"emails must be a non-empty array" ] ]));
    XCTAssertEqualObjects(_successes, @[]);
}

#pragma mark - Tags and user id

// Verifies that setTags() confirms to JS when the SDK accepts the tags.
- (void)testSetTagsConfirmsOnSuccess {
    __block PWCompletion completion = nil;
    NSDictionary *tags = @{ @"plan" : @"pro", @"visits" : @3 };
    OCMStub([self.pushManager setTags:tags withCompletion:[self captureCompletionInto:&completion]]);

    [self.plugin setTags:tags success:_success error:_error];
    completion(nil);

    XCTAssertEqualObjects(_successes, @[ @[] ]);
    XCTAssertEqualObjects(_errors, @[]);
}

// Verifies that setTags() hands the SDK error message to the JS error callback.
- (void)testSetTagsReportsFailure {
    __block PWCompletion completion = nil;
    OCMStub([self.pushManager setTags:[OCMArg any] withCompletion:[self captureCompletionInto:&completion]]);

    [self.plugin setTags:@{ @"plan" : @"pro" } success:_success error:_error];
    completion([self errorNamed:@"Network unavailable"]);

    XCTAssertEqualObjects(_errors, @[ @[ @"Network unavailable" ] ]);
    XCTAssertEqualObjects(_successes, @[]);
}

// Verifies that getTags() hands the device tags to JS as they came from the SDK.
- (void)testGetTagsDeliversTags {
    NSDictionary *tags = @{ @"plan" : @"pro", @"visits" : @3 };
    OCMStub(([self.pushManager loadTags:[OCMArg invokeBlockWithArgs:tags, nil] error:[OCMArg any]]));

    [self.plugin getTags:_success error:_error];

    XCTAssertEqualObjects(_successes, @[ @[ tags ] ]);
    XCTAssertEqualObjects(_errors, @[]);
}

// Verifies that setUserId() passes the id to the SDK and maps its completion onto the JS callbacks.
- (void)testSetUserIdMapsCompletionOntoCallbacks {
    __block PWCompletion completion = nil;
    OCMStub([self.pushwoosh setUserId:@"user-1" completion:[self captureCompletionInto:&completion]]);

    [self.plugin setUserId:@"user-1" success:_success error:_error];
    completion(nil);
    completion([self errorNamed:@"rejected"]);

    XCTAssertEqualObjects(_successes, @[ @[] ]);
    XCTAssertEqualObjects(_errors, @[ @[ @"rejected" ] ]);
}

#pragma mark - Inbox

- (id)inboxMessageWithCode:(NSString *)code {
    NSObject<PWInboxMessageProtocol> *message = OCMProtocolMock(@protocol(PWInboxMessageProtocol));
    OCMStub([message code]).andReturn(code);
    OCMStub([message title]).andReturn(@"Title");
    OCMStub([message message]).andReturn(@"Body");
    OCMStub([message type]).andReturn(PWInboxMessageTypeURL);
    OCMStub([message isRead]).andReturn(YES);
    OCMStub([message isActionPerformed]).andReturn(NO);
    OCMStub([message sendDate]).andReturn([NSDate dateWithTimeIntervalSince1970:1789000000]);
    OCMStub([message actionParams]).andReturn((@{ @"l" : @"https://link.example", @"u" : @"{\"promo\":42}" }));
    return message;
}

// Verifies that loadMessages() delivers the inbox to JS as an array of message dictionaries with
// the fields the typings promise and the custom data pulled out of the action params.
- (void)testLoadMessagesDeliversMessagesAsDictionaries {
    id inbox = OCMClassMock([PWInbox class]);
    [self addTeardownBlock:^{ [inbox stopMocking]; }];
    NSArray *messages = @[ [self inboxMessageWithCode:@"m-1"], [self inboxMessageWithCode:@"m-2"] ];
    OCMStub(ClassMethod(([inbox loadMessagesWithCompletion:[OCMArg invokeBlockWithArgs:messages, [NSNull null], nil]])));

    [self.plugin loadMessages:_success fail:_error];

    XCTAssertEqual(_successes.count, 1);
    NSArray<NSDictionary *> *delivered = ((NSArray *)_successes.firstObject).firstObject;
    XCTAssertEqual(delivered.count, 2);
    XCTAssertEqualObjects(delivered[0][@"code"], @"m-1");
    XCTAssertEqualObjects(delivered[1][@"code"], @"m-2");
    XCTAssertEqualObjects(delivered[0][@"title"], @"Title");
    XCTAssertEqualObjects(delivered[0][@"message"], @"Body");
    XCTAssertEqualObjects(delivered[0][@"type"], @(PWInboxMessageTypeURL));
    XCTAssertEqualObjects(delivered[0][@"isRead"], @YES);
    XCTAssertEqualObjects(delivered[0][@"isActionPerformed"], @NO);
    XCTAssertEqualObjects(delivered[0][@"customData"], @"{\"promo\":42}");
    XCTAssertTrue([delivered[0][@"sendDate"] length] > 0);
    XCTAssertEqualObjects(_errors, @[]);
}

// Verifies that a failed inbox load ends in the JS fail callback with the SDK message, instead
// of an empty inbox or a callback that never fires.
- (void)testLoadMessagesReportsFailure {
    id inbox = OCMClassMock([PWInbox class]);
    [self addTeardownBlock:^{ [inbox stopMocking]; }];
    OCMStub(ClassMethod(([inbox loadMessagesWithCompletion:[OCMArg invokeBlockWithArgs:[NSNull null], [self errorNamed:@"offline"], nil]])));

    [self.plugin loadMessages:_success fail:_error];

    XCTAssertEqualObjects(_errors, @[ @[ @"offline" ] ]);
    XCTAssertEqualObjects(_successes, @[]);
}

// Verifies that the style keys the JS API documents land on the matching PWIInboxStyle
// properties; a renamed key would silently leave the inbox unstyled.
- (void)testInboxStyleAppliesJsKeys {
    NSDictionary *styleDictionary = @{
        @"listEmptyMessage" : @"Nothing here yet",
        @"listErrorMessage" : @"Could not load",
        @"accentColor" : @(0xFF112233),
        @"barTextColor" : @(0xFF445566),
        @"dateFormat" : @"dd.MM.yyyy"
    };

#pragma clang diagnostic push
#pragma clang diagnostic ignored "-Warc-performSelector-leaks"
    PWIInboxStyle *style = [self.plugin performSelector:NSSelectorFromString(@"inboxStyleForDictionary:") withObject:styleDictionary];
#pragma clang diagnostic pop

    XCTAssertEqualObjects(style.listEmptyMessage, @"Nothing here yet");
    XCTAssertEqualObjects(style.listErrorMessage, @"Could not load");
    XCTAssertEqualObjects(style.accentColor, [RCTConvert UIColor:@(0xFF112233)]);
    XCTAssertEqualObjects(style.barTextColor, [RCTConvert UIColor:@(0xFF445566)]);
    // A fixed instant rather than one built from the runner's calendar, and the formatter is
    // pinned to the same zone the production block uses by default.
    NSDate *date = [NSDate dateWithTimeIntervalSince1970:1789000000];
    NSDateFormatter *expected = [NSDateFormatter new];
    expected.dateFormat = @"dd.MM.yyyy";
    XCTAssertEqualObjects(style.dateFormatterBlock(date, nil), [expected stringFromDate:date]);
}

#pragma mark - Rich Media

// Verifies that the JS RichMediaStyle constants (MODAL = 0, LEGACY = 1) map onto the SDK's
// presentation styles and read back as the same constant.
- (void)testRichMediaTypeRoundTripsJsConstants {
    __block PWRichMediaPresentationStyle stored = PWRichMediaPresentationStyleLegacy;
    id media = OCMClassMock([PWMedia class]);
    [self addTeardownBlock:^{ [media stopMocking]; }];
    OCMStub(ClassMethod([media setRichMediaPresentationStyle:PWRichMediaPresentationStyleModal])).ignoringNonObjectArgs().andDo(^(NSInvocation *invocation) {
        PWRichMediaPresentationStyle style;
        [invocation getArgument:&style atIndex:2];
        stored = style;
    });
    OCMStub(ClassMethod([media richMediaPresentationStyle])).andDo(^(NSInvocation *invocation) {
        PWRichMediaPresentationStyle style = stored;
        [invocation setReturnValue:&style];
    });

    [self.plugin setRichMediaType:0];
    XCTAssertEqual(stored, PWRichMediaPresentationStyleModal);
    [self.plugin getRichMediaType:_success];
    [self.plugin setRichMediaType:1];
    XCTAssertEqual(stored, PWRichMediaPresentationStyleLegacy);
    [self.plugin getRichMediaType:_success];

    XCTAssertEqualObjects(_successes, (@[ @[ @0 ], @[ @1 ] ]));
}

#pragma mark - Communication

// Verifies that setCommunicationEnabled() maps the flag onto start/stop of server communication,
// confirms to JS, and that isCommunicationEnabled() reads the setting back.
- (void)testCommunicationToggleReachesSdkAndReadsBack {
    NSMutableArray *states = [NSMutableArray array];
    RCTResponseSenderBlock readState = ^(NSArray *response) {
        [states addObject:response.firstObject];
    };

    [self.plugin setCommunicationEnabled:NO success:_success error:_error];
    [self.plugin isCommunicationEnabled:readState];
    [self.plugin setCommunicationEnabled:YES success:_success error:_error];
    [self.plugin isCommunicationEnabled:readState];

    OCMVerify([self.pushwoosh stopServerCommunication]);
    OCMVerify([self.pushwoosh startServerCommunication]);
    XCTAssertEqualObjects(states, (@[ @NO, @YES ]));
    XCTAssertEqualObjects(_successes, (@[ @[], @[] ]));
    XCTAssertEqualObjects(_errors, @[]);
}

#pragma mark - Local notifications

// Verifies that createLocalNotification() schedules a UNNotificationRequest carrying the JS
// message, delay and user data.
- (void)testCreateLocalNotificationSchedulesRequestFromJsParams {
    __block UNNotificationRequest *scheduled = nil;
    id center = OCMClassMock([UNUserNotificationCenter class]);
    [self addTeardownBlock:^{ [center stopMocking]; }];
    OCMStub(ClassMethod([center currentNotificationCenter])).andReturn(center);
    OCMStub([center addNotificationRequest:[OCMArg checkWithBlock:^BOOL(id request) {
        scheduled = request;
        return YES;
    }] withCompletionHandler:[OCMArg any]]);

    [self.plugin createLocalNotification:@{ @"msg" : @"Your order is ready", @"seconds" : @5, @"userData" : @{ @"orderId" : @"42" } }];

    XCTAssertNotNil(scheduled);
    XCTAssertEqualObjects(scheduled.content.body, @"Your order is ready");
    XCTAssertEqualObjects(scheduled.content.userInfo, @{ @"orderId" : @"42" });
    XCTAssertTrue([scheduled.trigger isKindOfClass:[UNTimeIntervalNotificationTrigger class]]);
    XCTAssertEqual(((UNTimeIntervalNotificationTrigger *)scheduled.trigger).timeInterval, 5);
}

@end
