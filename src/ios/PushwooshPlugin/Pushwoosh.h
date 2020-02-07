//
//  Pushwoosh.h
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import <Foundation/Foundation.h>

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

#if __has_include(<Pushwoosh/PushNotificationManager.h>)
#import <Pushwoosh/PushNotificationManager.h>
#import <Pushwoosh/PWInAppManager.h>
#import <PushwooshInboxUI/PushwooshInboxUI.h>
#import <Pushwoosh/PWGDPRManager.h>
#import <Pushwoosh/PWInlineInAppView.h>
#import <Pushwoosh/PWInbox.h>
#else
#import "PushNotificationManager.h"
#import "PWInAppManager.h"
#import "PushwooshInboxUI.h"
#import "PWGDPRManager.h"
#import "PWInlineInAppView.h"
#import "PWInbox.h"
#endif

@interface Pushwoosh: RCTEventEmitter<RCTBridgeModule,PushNotificationDelegate>

@end
