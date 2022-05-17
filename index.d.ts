declare module 'pushwoosh-react-native-plugin' {
  interface PushwooshConfig {
    pw_appid: string;
    project_number: string;
    pw_notification_handling?: string;
  }
  type LocalNotification = {
    msg: string;
    seconds: number;
    userData?: Object;
  }

  interface PushwooshTags {
    [index: string]: string | number | string[] | number[]
  }

  interface Pushwoosh {
    init(config: PushwooshConfig, success?: () => void, fail?: () => void): void;
    createLocalNotification(notification: LocalNotification): void;
    clearLocalNotification(): void;
    clearNotificationCenter(): void;
    register(success?: (token: string) => void, fail?: (error: Error) => void): void;
    unregister(success?: (token: string) => void, fail?: (error: Error) => void): void;
    onPushOpen(callback: () => void, fail?: ()=> void): void; 
    setTags(
      tags: Record<string, PushwooshTags>,
      success?: () => void,
      fail?: (error: Error) => void
    ): void;
    getTags(success: (tags: PushwooshTags) => void, fail?: (error: Error) => void): void;
    setShowPushnotificationAlert(showAlert: boolean): void;
    getShowPushnotificationAlert(callback: (willShow: boolean) => void): void;
    getPushToken(success?: (token: string) => void): void;
    getHwid(success: (hwid: string) => void): void;
    setUserId(userId: string, success?: ()=> void, fail?: (error: Error) => void): void;
    postEvent(event: string, attributes?: Record<string, string>): void;
    setApplicationIconBadgeNumber(badgeNumber: number): void;
    getApplicationIconBadgeNumber(callback: (badge: number) => void): void;
    addToApplicationIconBadgeNumber(badgeNumber: number): void;
    setMultiNotificationMode(on: boolean): void;
    setLightScreenOnNotification(on: boolean): void;
    setEnableLED(on: boolean): void;
    setColorLED(color: number): void;
    setSoundType(type: number): void;
    setVibrateType(type: number): void;
    presentInboxUI(style?: Object): void;
    showGDPRConsentUI(): void;
    showGDPRDeletionUI(): void;
    isDeviceDataRemoved(success: (isRemoved: boolean) => void): void;
    isCommunicationEnabled(success: (isEnabled: boolean)=> void): void;
    isAvailableGDPR(success: (isAvailable: boolean) => void): void;
    setCommunicationEnabled(enabled: boolean, success?: () => void, fail?: (error: Error) => void): void;
    removeAllDeviceData(success?: () => void, fail?: (error: Error) => void): void;
    setNotificationIconBackgroundColor(color: string): void;
    setLanguage(language: string): void;
    enableHuaweiPushNotifications(): void;
  }

  declare const Pushwoosh: Pushwoosh;
  export = Pushwoosh;
}