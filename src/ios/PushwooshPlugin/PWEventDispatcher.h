//
//  PWEventDispatcher.h
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import <Foundation/Foundation.h>

#import <React/RCTBridge.h>

@interface PWEventDispatcher : NSObject

+ (instancetype)sharedDispatcher;

- (void)subscribe:(RCTResponseSenderBlock)callback toEvent:(NSString*)event;

- (void)dispatchEvent:(NSString*)event withArgs:(NSArray*)args;

@end
