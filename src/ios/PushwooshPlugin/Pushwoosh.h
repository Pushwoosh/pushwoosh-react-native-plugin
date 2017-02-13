//
//  Pushwoosh.h
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import <Foundation/Foundation.h>

#import <React/RCTBridgeModule.h>
#import <React/RCTEventEmitter.h>

#import "PushNotificationManager.h"

@interface Pushwoosh: RCTEventEmitter<RCTBridgeModule,PushNotificationDelegate>

@end
