//
//  PWEventDispatcher.m
//  Pushwoosh React Native Plugin
//  (c) Pushwoosh 2016
//

#import "PWEventDispatcher.h"

@interface PWEventDispatcher()

@property (nonatomic, strong) NSMutableDictionary *subscribers;

@end

@implementation PWEventDispatcher

- (id)init {
	self = [super init];
	if (self) {
		_subscribers = [NSMutableDictionary new];
	}
	return self;
}

+ (instancetype)sharedDispatcher {
	static dispatch_once_t onceToken;
	static PWEventDispatcher *instance;
	dispatch_once(&onceToken, ^{
		instance = [PWEventDispatcher new];
	});
	
	return instance;
}

- (void)subscribe:(RCTResponseSenderBlock)callback toEvent:(NSString*)event {
	if (!callback) {
		return;
	}
	
	@synchronized(_subscribers) {
		if (!_subscribers[event]) {
			_subscribers[event] = [NSMutableArray new];
		}
		
		NSMutableArray *eventSubscribers = _subscribers[event];
		[eventSubscribers addObject:callback];
	}
}

- (void)dispatchEvent:(NSString*)event withArgs:(NSArray*)args {
	@synchronized(_subscribers) {
		NSMutableArray *eventSubscribers = _subscribers[event];
		for (RCTResponseSenderBlock callback in eventSubscribers) {
			callback(args);
		}
		
		// A native module is supposed to invoke its callback only once!
		[eventSubscribers removeAllObjects];
	}
}

@end
