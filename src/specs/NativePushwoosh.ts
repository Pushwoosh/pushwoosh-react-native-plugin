import type {TurboModule} from 'react-native';
import {TurboModuleRegistry} from 'react-native';

// Codegen input. Parameter names become the Objective-C selector labels (init:success:error:)
// and the Java parameter names, so they must match the native implementations.
export interface Spec extends TurboModule {
  setReverseProxy(url: string, headers: Object | null): void;
  init(config: Object, success: (result: Object) => void, error: (message: string) => void): void;
  // `register` is a C++ keyword, so codegen cannot emit a method of that name
  registerForPushNotifications(success: (token: string) => void, error: (message: string) => void): void;
  unregister(success: (token: string) => void, error: (message: string) => void): void;
  onPushOpen(callback: (push: Object) => void): void;
  addListener(eventName: string): void;
  removeListeners(count: number): void;
  setEmails(emails: Array<string>, success: () => void, error: (message: string) => void): void;
  setUserEmails(
    userId: string,
    emails: Array<string>,
    success: () => void,
    error: (message: string) => void,
  ): void;
  registerSMSNumber(phoneNumber: string): void;
  registerWhatsappNumber(phoneNumber: string): void;
  setTags(tags: Object, success: () => void, error: (message: string) => void): void;
  getTags(success: (tags: Object) => void, error: (message: string) => void): void;
  setShowPushnotificationAlert(showPushnotificationAlert: boolean): void;
  getShowPushnotificationAlert(callback: (showPushnotificationAlert: boolean) => void): void;
  getPushToken(callback: (token: string) => void): void;
  getHwid(callback: (hwid: string) => void): void;
  getUserId(callback: (userId: string) => void): void;
  setUserId(userId: string, success: () => void, error: (message: string) => void): void;
  postEvent(event: string, attributes: Object): void;
  setApplicationIconBadgeNumber(badgeNumber: number): void;
  getApplicationIconBadgeNumber(callback: (badgeNumber: number) => void): void;
  addToApplicationIconBadgeNumber(badgeNumber: number): void;
  setMultiNotificationMode(on: boolean): void;
  setLightScreenOnNotification(on: boolean): void;
  setEnableLED(on: boolean): void;
  setColorLED(color: number): void;
  setSoundType(type: number): void;
  setVibrateType(type: number): void;
  presentInboxUI(style: Object | null): void;
  messagesWithNoActionPerformedCount(callback: (count: number) => void): void;
  unreadMessagesCount(callback: (count: number) => void): void;
  messagesCount(callback: (count: number) => void): void;
  loadMessages(success: (messages: Array<Object>) => void, fail: (message: string) => void): void;
  readMessage(code: string): void;
  readMessages(codes: Array<string>): void;
  deleteMessage(code: string): void;
  deleteMessages(codes: Array<string>): void;
  performAction(code: string): void;
  isCommunicationEnabled(callback: (enabled: boolean) => void): void;
  setCommunicationEnabled(
    enabled: boolean,
    success: () => void,
    error: (message: string) => void,
  ): void;
  setNotificationIconBackgroundColor(color: string): void;
  setLanguage(language: string): void;
  enableHuaweiPushNotifications(): void;
  setRichMediaType(type: number): void;
  getRichMediaType(callback: (type: number) => void): void;
  createLocalNotification(params: Object): void;
  clearLocalNotification(): void;
  clearNotificationCenter(): void;
}

// `get`, not `getEnforcing`: importing the package on a build without the native module (Jest,
// an unlinked app) must not throw; a method call does, as it always did.
//
// The module is named after its Objective-C class. React Native resolves a TurboModule by
// NSClassFromString(name) first, and "Pushwoosh" is the native SDK's own class.
export default TurboModuleRegistry.get<Spec>('PushwooshPlugin');
