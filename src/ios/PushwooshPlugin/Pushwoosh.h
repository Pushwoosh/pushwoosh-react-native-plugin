//
//  Pushwoosh.h
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import <Foundation/Foundation.h>

#import "RCTBridgeModule.h"
#import "RCTEventEmitter.h"

#import "PushNotificationManager.h"

@interface Pushwoosh: RCTEventEmitter<RCTBridgeModule,PushNotificationDelegate>

@end
