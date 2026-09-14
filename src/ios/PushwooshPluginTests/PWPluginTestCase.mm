//
//  PWPluginTestCase.mm
//  PushwooshPluginTests
//

#import "PWPluginTestCase.h"

#import <objc/message.h>

#import <React/RCTBridgeModuleDecorator.h>
#import <React/RCTLinkingManager.h>
#import <pushwoosh-react-native-plugin/PWEventDispatcher.h>

static NSString *const PWCommunicationEnabledDefaultsKey = @"PushwooshCommunicationEnabled";

@implementation PWPluginTestCase {
    RCTCallableJSModules *_callableJSModules;
    id<UNUserNotificationCenterDelegate> _originalNotificationCenterDelegate;
}

- (void)setUp {
    [super setUp];

    _originalNotificationCenterDelegate = [UNUserNotificationCenter currentNotificationCenter].delegate;

    _pushManager = OCMClassMock([PushNotificationManager class]);
    OCMStub(ClassMethod([_pushManager pushManager])).andReturn(_pushManager);

    _pushwoosh = OCMClassMock([Pushwoosh class]);
    OCMStub(ClassMethod([_pushwoosh sharedInstance])).andReturn(_pushwoosh);

    _jsEvents = [NSMutableArray array];
    _plugin = [PushwooshPlugin new];
    [self attachJSEventRecorder];
}

- (void)tearDown {
    // The launch push and its deep link live in statics of Pushwoosh.mm. Drain what the test left
    // behind so the next one starts clean: being started from a push again clears the "already
    // replayed" flag, onPushOpen: consumes the push and getInitialURL the link. Without the first
    // step a test that replays a launch push would leave the flag set and the next one - which
    // expects its own push replayed - would depend on the order XCTest ran them in.
    [self simulateLaunchPush:@{}];
    [self.plugin onPushOpen:^(NSArray *response) {}];
    [self initialURLFromLinkingManager];
    [[[PWEventDispatcher sharedDispatcher] valueForKey:@"subscribers"] removeAllObjects];

    [[NSUserDefaults standardUserDefaults] removeObjectForKey:PWCommunicationEnabledDefaultsKey];
    [UNUserNotificationCenter currentNotificationCenter].delegate = _originalNotificationCenterDelegate;

    [_pushManager stopMocking];
    [_pushwoosh stopMocking];
    _plugin = nil;

    [super tearDown];
}

// Bridgeless hands every module an RCTCallableJSModules through RCTBridgeModuleDecorator and
// RCTEventEmitter emits through it; the recorder stands where the JS runtime would.
- (void)attachJSEventRecorder {
    NSMutableArray<NSDictionary *> *events = _jsEvents;
    _callableJSModules = [RCTCallableJSModules new];
    [_callableJSModules setBridgelessJSModuleMethodInvoker:^(NSString *moduleName, NSString *methodName, NSArray *args, dispatch_block_t onComplete) {
        if ([moduleName isEqualToString:@"RCTDeviceEventEmitter"] && [methodName isEqualToString:@"emit"] && args.count > 0) {
            [events addObject:@{ @"name" : args[0], @"body" : args.count > 1 ? args[1] : [NSNull null] }];
        }
        if (onComplete) {
            onComplete();
        }
    }];

    RCTBridgeModuleDecorator *decorator = [[RCTBridgeModuleDecorator alloc] initWithViewRegistry:nil
                                                                                  moduleRegistry:nil
                                                                                   bundleManager:nil
                                                                               callableJSModules:_callableJSModules];
    [decorator attachInteropAPIsToModule:_plugin];
}

- (NSArray *)jsEventBodiesNamed:(NSString *)name {
    NSMutableArray *bodies = [NSMutableArray array];
    for (NSDictionary *event in _jsEvents) {
        if ([event[@"name"] isEqualToString:name]) {
            [bodies addObject:event[@"body"]];
        }
    }
    return bodies;
}

- (void)stubSDKInitialization {
    OCMStub(ClassMethod([self.pushManager initializeWithAppCode:[OCMArg any] appName:[OCMArg any]]));
}

- (void)initPluginWithAppCode:(NSString *)appCode {
    [self stubSDKInitialization];
    [self.plugin init:@{ @"pw_appid" : appCode } success:nil error:nil];
}

- (void)simulateLaunchPush:(NSDictionary *)push {
    [[UIApplication sharedApplication] onPushAccepted:nil withNotification:push onStart:YES];
}

- (id)initialURLFromLinkingManager {
    __block id resolved = nil;
    RCTPromiseResolveBlock resolve = ^(id value) {
        resolved = value;
    };
    RCTPromiseRejectBlock reject = ^(NSString *code, NSString *message, NSError *error) {
        resolved = error ?: message;
    };

    // getInitialURL:reject: is an RCT_EXPORT_METHOD of RCTLinkingManager, swizzled by the plugin;
    // it has no declaration to call through.
    RCTLinkingManager *linkingManager = [RCTLinkingManager new];
    SEL selector = NSSelectorFromString(@"getInitialURL:reject:");
    ((void (*)(id, SEL, RCTPromiseResolveBlock, RCTPromiseRejectBlock))objc_msgSend)(linkingManager, selector, resolve, reject);
    return resolved;
}

@end
