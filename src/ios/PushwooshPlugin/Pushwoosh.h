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
#import <PushwooshFramework/PWInAppManager.h>
#import <PushwooshInboxUI/PushwooshInboxUI.h>
#import <PushwooshFramework/PWGDPRManager.h>
#import <PushwooshFramework/PWInlineInAppView.h>
#import <PushwooshFramework/PWInbox.h>
#else
#import "PushNotificationManager.h"
#import "PWInAppManager.h"
#import "PushwooshInboxUI.h"
#import "PWGDPRManager.h"
#import "PWInlineInAppView.h"
#import "PWInbox.h"
#endif

@interface PushwooshPlugin: RCTEventEmitter<RCTBridgeModule, PushNotificationDelegate>

@end
