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
  var reactNativeDelegate: ReactNativeDelegate?
  var reactNativeFactory: RCTReactNativeFactory?

  // UNUserNotificationCenter holds its delegate weakly.
  var foreignNotificationDelegate: DemoForeignNotificationDelegate?
  // React Native started by a background launch, waiting for a scene to show it.
  var backgroundWindow: UIWindow?

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

    // A background launch (a silent push, say) connects no scene; a foreground one connects it first.
    DispatchQueue.main.async {
      guard application.connectedScenes.isEmpty else {
        return
      }
      let window = UIWindow(frame: UIScreen.main.bounds)
      factory.startReactNative(withModuleName: "demoapp", in: window, launchOptions: launchOptions)
      self.backgroundWindow = window
    }

    return true
  }
}

// The iOS 27 SDK requires the scene life cycle: the scene owns the window and receives the links.
class SceneDelegate: UIResponder, UIWindowSceneDelegate {
  var window: UIWindow?

  func scene(
    _ scene: UIScene,
    willConnectTo session: UISceneSession,
    options connectionOptions: UIScene.ConnectionOptions
  ) {
    guard let windowScene = scene as? UIWindowScene,
          let appDelegate = UIApplication.shared.delegate as? AppDelegate,
          let factory = appDelegate.reactNativeFactory else {
      return
    }

    let window = UIWindow(windowScene: windowScene)
    self.window = window

    if let backgroundWindow = appDelegate.backgroundWindow {
      window.rootViewController = backgroundWindow.rootViewController
      backgroundWindow.rootViewController = nil
      appDelegate.backgroundWindow = nil
      window.makeKeyAndVisible()
      return
    }

    factory.startReactNative(
      withModuleName: "demoapp",
      in: window,
      launchOptions: launchOptions(from: connectionOptions)
    )
  }

  // The app's own scheme and its Universal Links both go to JS; a foreign http link opens in Safari (SDK-884).
  func scene(_ scene: UIScene, openURLContexts URLContexts: Set<UIOpenURLContext>) {
    guard let url = URLContexts.first?.url else {
      return
    }
    RCTLinkingManager.application(UIApplication.shared, open: url, options: [:])
  }

  func scene(_ scene: UIScene, continue userActivity: NSUserActivity) {
    RCTLinkingManager.application(UIApplication.shared, continue: userActivity, restorationHandler: { _ in })
  }

  // Linking.getInitialURL() reads the launch URL from launchOptions; a scene gets it in connectionOptions.
  private func launchOptions(from connectionOptions: UIScene.ConnectionOptions) -> [UIApplication.LaunchOptionsKey: Any] {
    var launchOptions: [UIApplication.LaunchOptionsKey: Any] = [:]
    if let url = connectionOptions.urlContexts.first?.url {
      launchOptions[.url] = url
    }
    if let activity = connectionOptions.userActivities.first(where: { $0.activityType == NSUserActivityTypeBrowsingWeb }) {
      launchOptions[.userActivityDictionary] = [
        UIApplication.LaunchOptionsKey.userActivityType.rawValue: activity.activityType,
        "UIApplicationLaunchOptionsUserActivityKey": activity,
      ]
    }
    return launchOptions
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
