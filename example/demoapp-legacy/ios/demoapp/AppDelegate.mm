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

@interface AppDelegate ()
- (void)startReactNativeWithLaunchOptions:(NSDictionary *)launchOptions;
@end

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

  // A background launch (a silent push, say) connects no scene; a foreground one connects it first.
  dispatch_async(dispatch_get_main_queue(), ^{
    if (application.connectedScenes.count == 0) {
      [self startReactNativeWithLaunchOptions:launchOptions];
    }
  });
  return YES;
}

// Linking.getInitialURL() reads the bridge's launchOptions, and a scene app gets the launch URL only
// in connectionOptions, so React Native starts once the scene connects.
- (void)startReactNativeWithLaunchOptions:(NSDictionary *)launchOptions
{
  if (self.rootViewFactory) {
    return;
  }
  [super application:[UIApplication sharedApplication] didFinishLaunchingWithOptions:launchOptions];
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

// The iOS 27 SDK requires the scene life cycle. RCTAppDelegate builds the React Native root in a
// window of its own; the scene starts it, moves the root into the scene's window and receives the links.
@interface SceneDelegate : UIResponder <UIWindowSceneDelegate>
@property (nonatomic, strong) UIWindow *window;
@end

@implementation SceneDelegate

- (void)scene:(UIScene *)scene willConnectToSession:(UISceneSession *)session options:(UISceneConnectionOptions *)connectionOptions
{
  if (![scene isKindOfClass:[UIWindowScene class]]) {
    return;
  }

  AppDelegate *appDelegate = (AppDelegate *)[UIApplication sharedApplication].delegate;
  [appDelegate startReactNativeWithLaunchOptions:[self launchOptionsFromConnectionOptions:connectionOptions]];
  UIViewController *rootViewController = appDelegate.window.rootViewController;
  appDelegate.window.rootViewController = nil;

  UIWindow *window = [[UIWindow alloc] initWithWindowScene:(UIWindowScene *)scene];
  window.rootViewController = rootViewController;
  self.window = window;
  appDelegate.window = window;
  [window makeKeyAndVisible];
}

// RCTAppDelegate refreshes Dimensions from this callback; the scene no longer belongs to it.
- (void)windowScene:(UIWindowScene *)windowScene
    didUpdateCoordinateSpace:(id<UICoordinateSpace>)previousCoordinateSpace
        interfaceOrientation:(UIInterfaceOrientation)previousInterfaceOrientation
             traitCollection:(UITraitCollection *)previousTraitCollection
{
  id<UIWindowSceneDelegate> appDelegate = (id<UIWindowSceneDelegate>)[UIApplication sharedApplication].delegate;
  if ([appDelegate respondsToSelector:_cmd]) {
    [appDelegate windowScene:windowScene
        didUpdateCoordinateSpace:previousCoordinateSpace
            interfaceOrientation:previousInterfaceOrientation
                 traitCollection:previousTraitCollection];
  }
}

// The app's own scheme and its Universal Links both go to JS; a foreign http link opens in Safari (SDK-884).
- (void)scene:(UIScene *)scene openURLContexts:(NSSet<UIOpenURLContext *> *)URLContexts
{
  NSURL *url = URLContexts.anyObject.URL;
  if (url) {
    [RCTLinkingManager application:[UIApplication sharedApplication] openURL:url options:@{}];
  }
}

- (void)scene:(UIScene *)scene continueUserActivity:(NSUserActivity *)userActivity
{
  [RCTLinkingManager application:[UIApplication sharedApplication]
            continueUserActivity:userActivity
              restorationHandler:^(NSArray<id<UIUserActivityRestoring>> *restorableObjects) {}];
}

- (NSDictionary *)launchOptionsFromConnectionOptions:(UISceneConnectionOptions *)connectionOptions
{
  NSMutableDictionary *launchOptions = [NSMutableDictionary dictionary];
  NSURL *url = connectionOptions.URLContexts.anyObject.URL;
  if (url) {
    launchOptions[UIApplicationLaunchOptionsURLKey] = url;
  }
  for (NSUserActivity *activity in connectionOptions.userActivities) {
    if ([activity.activityType isEqualToString:NSUserActivityTypeBrowsingWeb]) {
      launchOptions[UIApplicationLaunchOptionsUserActivityDictionaryKey] = @{
        UIApplicationLaunchOptionsUserActivityTypeKey : activity.activityType,
        @"UIApplicationLaunchOptionsUserActivityKey" : activity,
      };
      break;
    }
  }
  return launchOptions;
}

@end
