import UIKit
import React
import React_RCTAppDelegate
import ReactAppDependencyProvider
import UserNotifications

// Stands in for a third-party SDK (an analytics one, say) that claims
// UNUserNotificationCenter.delegate in didFinishLaunchingWithOptions, which is what most real
// integrations look like. Without it the delegate slot is empty when the SDK starts, the SDK falls
// back to reading the launch push from launchOptions, and the cold start path taken under
// Pushwoosh_PLUGIN_NOTIFICATION_HANDLER is never exercised — the very path a customer hit.
class DemoForeignNotificationDelegate: NSObject, UNUserNotificationCenterDelegate {
  weak var previousDelegate: UNUserNotificationCenterDelegate?

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    didReceive response: UNNotificationResponse,
    withCompletionHandler completionHandler: @escaping () -> Void
  ) {
    NSLog("[demoapp] foreign delegate got a notification response")

    if let previous = previousDelegate,
       previous.responds(to: #selector(UNUserNotificationCenterDelegate.userNotificationCenter(_:didReceive:withCompletionHandler:))) {
      previous.userNotificationCenter?(center, didReceive: response, withCompletionHandler: completionHandler)
    } else {
      completionHandler()
    }
  }

  func userNotificationCenter(
    _ center: UNUserNotificationCenter,
    willPresent notification: UNNotification,
    withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
  ) {
    NSLog("[demoapp] foreign delegate got a foreground notification")

    if let previous = previousDelegate,
       previous.responds(to: #selector(UNUserNotificationCenterDelegate.userNotificationCenter(_:willPresent:withCompletionHandler:))) {
      previous.userNotificationCenter?(center, willPresent: notification, withCompletionHandler: completionHandler)
    } else {
      completionHandler([])
    }
  }
}

@main
class AppDelegate: UIResponder, UIApplicationDelegate {
  var window: UIWindow?

  var reactNativeDelegate: ReactNativeDelegate?
  var reactNativeFactory: RCTReactNativeFactory?

  // UNUserNotificationCenter holds its delegate weakly.
  var foreignNotificationDelegate: DemoForeignNotificationDelegate?

  func application(
    _ application: UIApplication,
    didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
  ) -> Bool {
    let notificationCenter = UNUserNotificationCenter.current()
    let foreignDelegate = DemoForeignNotificationDelegate()
    foreignDelegate.previousDelegate = notificationCenter.delegate
    notificationCenter.delegate = foreignDelegate
    foreignNotificationDelegate = foreignDelegate

    let delegate = ReactNativeDelegate()
    let factory = RCTReactNativeFactory(delegate: delegate)
    delegate.dependencyProvider = RCTAppDependencyProvider()

    reactNativeDelegate = delegate
    reactNativeFactory = factory

    window = UIWindow(frame: UIScreen.main.bounds)

    factory.startReactNative(
      withModuleName: "demoapp",
      in: window,
      launchOptions: launchOptions
    )

    return true
  }

  // Only the app's own scheme is routed to JS. The Universal Links hook
  // (application:continueUserActivity:) is deliberately absent: RCTLinkingManager answers YES to
  // every BrowsingWeb activity, which makes the SDK treat an http link to someone else's site as
  // handled and the browser never opens (SDK-916).
  func application(
    _ app: UIApplication,
    open url: URL,
    options: [UIApplication.OpenURLOptionsKey: Any] = [:]
  ) -> Bool {
    return RCTLinkingManager.application(app, open: url, options: options)
  }
}

class ReactNativeDelegate: RCTDefaultReactNativeFactoryDelegate {
  override func sourceURL(for bridge: RCTBridge) -> URL? {
    self.bundleURL()
  }

  override func bundleURL() -> URL? {
#if DEBUG
    RCTBundleURLProvider.sharedSettings().jsBundleURL(forBundleRoot: "index")
#else
    Bundle.main.url(forResource: "main", withExtension: "jsbundle")
#endif
  }
}
