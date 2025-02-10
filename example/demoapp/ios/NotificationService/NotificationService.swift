//
//  NotificationService.swift
//  NotificationService
//
//  Created by Andrew Kis on 15.5.24..
//

import UserNotifications
import PushwooshFramework

/**
 Setting up Badges for Flutter
 docs: https://docs.pushwoosh.com/platform-docs/pushwoosh-sdk/cross-platform-frameworks/react-native/integrating-react-native-plugin
 
 1.  Add App Groups capability to both Runner and NotificationService targets and add a new group with the same name for both targets
 2. Add PW_APP_GROUPS_NAME info.plist flag to both Main Target and NotificationService targets with the group name as its string value
 7. Add code
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
