#import "AppDelegate.h"

#import <React/RCTBundleURLProvider.h>
#import <React/RCTLinkingManager.h>
#import <UserNotifications/UserNotifications.h>

// Stands in for a third-party SDK (an analytics one, say) that claims
// UNUserNotificationCenter.delegate in didFinishLaunchingWithOptions, which is what most real
// integrations look like. Without it the delegate slot is empty when the SDK starts, the SDK falls
// back to reading the launch push from launchOptions, and the cold start path taken under
// Pushwoosh_PLUGIN_NOTIFICATION_HANDLER is never exercised — the very path a customer hit.
@interface DemoForeignNotificationDelegate : NSObject <UNUserNotificationCenterDelegate>
@property (nonatomic, weak) id<UNUserNotificationCenterDelegate> previousDelegate;
@end

@implementation DemoForeignNotificationDelegate

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
didReceiveNotificationResponse:(UNNotificationResponse *)response
         withCompletionHandler:(void (^)(void))completionHandler
{
  NSLog(@"[demoapp] foreign delegate got a notification response");

  if ([self.previousDelegate respondsToSelector:@selector(userNotificationCenter:didReceiveNotificationResponse:withCompletionHandler:)]) {
    [self.previousDelegate userNotificationCenter:center didReceiveNotificationResponse:response withCompletionHandler:completionHandler];
  } else {
    completionHandler();
  }
}

- (void)userNotificationCenter:(UNUserNotificationCenter *)center
       willPresentNotification:(UNNotification *)notification
         withCompletionHandler:(void (^)(UNNotificationPresentationOptions))completionHandler
{
  NSLog(@"[demoapp] foreign delegate got a foreground notification");

  if ([self.previousDelegate respondsToSelector:@selector(userNotificationCenter:willPresentNotification:withCompletionHandler:)]) {
    [self.previousDelegate userNotificationCenter:center willPresentNotification:notification withCompletionHandler:completionHandler];
  } else {
    completionHandler(UNNotificationPresentationOptionNone);
  }
}

@end

// UNUserNotificationCenter holds its delegate weakly.
static DemoForeignNotificationDelegate *gForeignNotificationDelegate = nil;

@implementation AppDelegate

- (BOOL)application:(UIApplication *)application didFinishLaunchingWithOptions:(NSDictionary *)launchOptions
{
  self.moduleName = @"demoapp";
  // You can add your custom initial props in the dictionary below.
  // They will be passed down to the ViewController used by React Native.
  self.initialProps = @{};

  UNUserNotificationCenter *notificationCenter = [UNUserNotificationCenter currentNotificationCenter];
  gForeignNotificationDelegate = [DemoForeignNotificationDelegate new];
  gForeignNotificationDelegate.previousDelegate = notificationCenter.delegate;
  notificationCenter.delegate = gForeignNotificationDelegate;

  return [super application:application didFinishLaunchingWithOptions:launchOptions];
}

// Only the app's own scheme is routed to JS. The Universal Links hook
// (application:continueUserActivity:) is deliberately absent: RCTLinkingManager answers YES to
// every BrowsingWeb activity, which makes the SDK treat an http link to someone else's site as
// handled and the browser never opens (SDK-916).
- (BOOL)application:(UIApplication *)application
            openURL:(NSURL *)url
            options:(NSDictionary<UIApplicationOpenURLOptionsKey, id> *)options
{
  return [RCTLinkingManager application:application openURL:url options:options];
}

- (NSURL *)sourceURLForBridge:(RCTBridge *)bridge
{
  return [self bundleURL];
}

- (NSURL *)bundleURL
{
#if DEBUG
  return [[RCTBundleURLProvider sharedSettings] jsBundleURLForBundleRoot:@"index"];
#else
  return [[NSBundle mainBundle] URLForResource:@"main" withExtension:@"jsbundle"];
#endif
}

@end
