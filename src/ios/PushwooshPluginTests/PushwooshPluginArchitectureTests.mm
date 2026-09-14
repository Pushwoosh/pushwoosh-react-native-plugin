//
//  PushwooshPluginArchitectureTests.mm
//  PushwooshPluginTests
//
//  Which React Native architecture the module was built for. One of the two tests applies per
//  build; tools/ios-test.bash checks that the one expected for its matrix row passed.
//

#import "PWPluginTestCase.h"

#import <React/RCTBridge.h>

@interface PushwooshPluginArchitectureTests : PWPluginTestCase
@end

@implementation PushwooshPluginArchitectureTests

- (BOOL)builtForNewArchitecture {
    Protocol *spec = NSProtocolFromString(@"NativePushwooshSpec");
    return spec != nil && [PushwooshPlugin conformsToProtocol:spec];
}

// Verifies that with the New Architecture on, the module implements the codegen spec and hands
// out the TurboModule React Native asks for; nothing else makes it a TurboModule.
- (void)testModuleIsTurboModule {
    XCTSkipUnless([self builtForNewArchitecture], @"the plugin was built for the legacy bridge");

    XCTAssertTrue([self.plugin respondsToSelector:NSSelectorFromString(@"getTurboModule:")]);
    XCTAssertTrue([PushwooshPlugin conformsToProtocol:@protocol(RCTBridgeModule)]);
}

// Verifies that with the New Architecture off, the module is a plain bridge module and carries
// no TurboModule factory, so the legacy bridge owns it.
- (void)testModuleIsLegacyBridgeModule {
    XCTSkipIf([self builtForNewArchitecture], @"the plugin was built for the New Architecture");

    XCTAssertFalse([self.plugin respondsToSelector:NSSelectorFromString(@"getTurboModule:")]);
    XCTAssertTrue([PushwooshPlugin conformsToProtocol:@protocol(RCTBridgeModule)]);
}

// Verifies that the module is registered under its class name: React Native resolves a
// TurboModule with NSClassFromString(name) before it consults the RCT_EXPORT_MODULE registry, JS
// asks for "PushwooshPlugin", and "Pushwoosh" is the SDK's own class.
- (void)testModuleIsNamedAfterItsClass {
    NSString *name = RCTBridgeModuleNameForClass([PushwooshPlugin class]);

    XCTAssertEqualObjects(name, @"PushwooshPlugin");
    XCTAssertEqualObjects(name, NSStringFromClass([PushwooshPlugin class]));
    XCTAssertNotEqualObjects(name, NSStringFromClass([Pushwoosh class]));
}

@end
