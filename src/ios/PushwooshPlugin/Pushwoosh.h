//
//  Pushwoosh.h
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import <Foundation/Foundation.h>

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>
#import <PushwooshFramework/PushwooshFramework.h>

#if __has_include(<PushwooshFramework/PushNotificationManager.h>)
#import <PushwooshFramework/PushNotificationManager.h>
#import <PushwooshFramework/PWInbox.h>
#import <PushwooshCore/PWInAppManager.h>
#import <PushwooshCore/PWMedia.h>
#import <PushwooshInboxUI/PushwooshInboxUI.h>
#else
#import "PushNotificationManager.h"
#import "PWInbox.h"
#import "PWInAppManager.h"
#import "PWMedia.h"
#import "PushwooshInboxUI.h"
#endif

#ifdef RCT_NEW_ARCH_ENABLED
#import <RNPushwooshSpec/RNPushwooshSpec.h>

@interface PushwooshPlugin: RCTEventEmitter<NativePushwooshSpec, PushNotificationDelegate>
#else
@interface PushwooshPlugin: RCTEventEmitter<RCTBridgeModule, PushNotificationDelegate>
#endif

@end
