//
//  Pushwoosh.h
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import <Foundation/Foundation.h>

#import "RCTBridgeModule.h"
#import "PushNotificationManager.h"

@interface Pushwoosh: NSObject<RCTBridgeModule,PushNotificationDelegate>

@end
