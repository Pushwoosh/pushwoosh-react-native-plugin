//
//  NotificationService.swift
//  NotificationService
//
//  Created by Andrew Kis on 15.5.24..
//

import UserNotifications
import PushwooshFramework

/**
 This demo deliberately leaves the extension without the application code: the App Group
 entitlements of both targets are empty, PW_APP_GROUPS_NAME is empty in both Info.plist files and
 the app's Info.plist has no Pushwoosh_APPID. The code comes only from Pushwoosh.init() in JS, as in
 a typical React Native app, and the extension, a separate process, does not see it.

 So the extension sends no delivery event (messageDeliveryEvent), and with iOS SDK 7.0.36 to 7.2.8
 every notification is shown only after about 30 seconds, when the extension times out. That is
 expected here, not a bug of the demo (SDK-1057): the extension setup is left to the app.

 To give the extension the application code in an app, do one of:
 1. Add Pushwoosh_APPID to the app's Info.plist. The extension inherits it (iOS SDK 7.1.0 and newer).
 2. Add the same App Group to the app and NotificationService targets and set PW_APP_GROUPS_NAME to
    its identifier in both Info.plist files. The App Group also keeps the badge in sync.
 Details: "Notification Service Extension" in the plugin README (public/README.md).
 */

class NotificationService: UNNotificationServiceExtension {
  
  var contentHandler: ((UNNotificationContent) -> Void)?
  var bestAttemptContent: UNMutableNotificationContent?
  
  override func didReceive(_ request: UNNotificationRequest, withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void) {
    
    PWNotificationExtensionManager.shared().handle(request, contentHandler: contentHandler)
  }
  
  override func serviceExtensionTimeWillExpire() {
    // Called just before the extension will be terminated by the system.
    // Use this as an opportunity to deliver your "best attempt" at modified content, otherwise the original push payload will be used.
    if let contentHandler = contentHandler, let bestAttemptContent =  bestAttemptContent {
      contentHandler(bestAttemptContent)
    }
  }
  
}
