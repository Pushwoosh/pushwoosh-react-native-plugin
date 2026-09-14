package com.pushwoosh.reactnativeplugin;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.badge.PushwooshBadge;
import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.SetEmailException;
import com.pushwoosh.exception.SetUserException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.InAppManager;
import com.pushwoosh.inbox.PushwooshInbox;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationSettings;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.notification.LocalNotificationReceiver;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class PushwooshPlugin extends PushwooshPluginSpec implements LifecycleEventListener {

	static final String TAG = "ReactNativePlugin";
	// Same as the iOS class name: React Native resolves a TurboModule by class name first
	public static final String MODULE_NAME = "PushwooshPlugin";
	private static final String PUSH_OPEN_EVENT = "PwPushOpened";
	private static final String PUSH_OPEN_JS_EVENT = "pushOpened";

	private static final String PUSH_RECEIVED_EVENT = Pushwoosh.PUSH_RECEIVE_EVENT;
	private static final String PUSH_RECEIVED_JS_EVENT = "pushReceived";

	private static EventDispatcher mEventDispatcher = new EventDispatcher();

	private static String sReceivedPushData;
	private static boolean sReceivedPushCallbackRegistered = false;

	private static String sStartPushData;
	private static boolean sPushCallbackRegistered = false;

	private static boolean sInitialized = false;
	private static final Object sStartPushLock = new Object();

	private static PushwooshPlugin INSTANCE = null;

	private InboxUiStyleManager inboxUiInboxUiStyleManager;

	public PushwooshPlugin(ReactApplicationContext reactContext) {
		super(reactContext);

		INSTANCE = this;

		reactContext.addLifecycleEventListener(this);

		Context applicationContext = AndroidPlatformModule.getApplicationContext();
		inboxUiInboxUiStyleManager = new InboxUiStyleManager(applicationContext);
	}

	///
	/// Module API
	///

	@Override
	public String getName() {
		return MODULE_NAME;
	}

	@ReactMethod
	public void setReverseProxy(String url, ReadableMap headers) {
		Map<String, String> headersMap = null;
		if (headers != null) {
			headersMap = new HashMap<>();
			for (Map.Entry<String, Object> entry : headers.toHashMap().entrySet()) {
				headersMap.put(entry.getKey(), String.valueOf(entry.getValue()));
			}
		}
		Pushwoosh.getInstance().setReverseProxy(url, headersMap);
	}

	@ReactMethod
	public void init(ReadableMap config, Callback success, Callback error) {
		String appId = config.hasKey("pw_appid") ? config.getString("pw_appid") : null;

		if (appId == null) {
			if (error != null) {
				error.invoke("Pushwoosh Application id not specified");
			}
			return;
		}

		Pushwoosh.getInstance().setAppId(appId);

		synchronized (sStartPushLock) {
			if (sReceivedPushData != null) {
				sendEvent(PUSH_RECEIVED_JS_EVENT, ConversionUtil.stringToJSONObject(sReceivedPushData));
			}

			if (sStartPushData != null) {
				sendEvent(PUSH_OPEN_JS_EVENT, ConversionUtil.stringToJSONObject(sStartPushData));
			}

			// Under the same lock as the replay above: a push taking the lock between the replay
			// and this flag would find sInitialized false, skip its own send, and never be
			// replayed either.
			sInitialized = true;
		}

		if (success != null) {
			success.invoke();
		}
	}

	@ReactMethod
	public void registerForPushNotifications(final Callback success, final Callback error) {
		Pushwoosh.getInstance().registerForPushNotifications(new RegisterForPushNotificationCallback(success, error));
	}

	@ReactMethod
	public void unregister(final Callback success, final Callback error) {
		Pushwoosh.getInstance().unregisterForPushNotifications(new com.pushwoosh.function.Callback<String, UnregisterForPushNotificationException>() {
			@Override
			public void process(Result<String, UnregisterForPushNotificationException> result) {
				if (result.isSuccess()) {
					if (success != null) {
						success.invoke(result.getData());
					}
				} else if (result.getException() != null) {
					if (error != null) {
						error.invoke(result.getException().getLocalizedMessage());
					}
				}
			}
		});
	}

	@ReactMethod
	public void onPushOpen(Callback callback) {
		synchronized (sStartPushLock) {
			if (!sPushCallbackRegistered && sStartPushData != null) {
				callback.invoke(ConversionUtil.toWritableMap(ConversionUtil.stringToJSONObject(sStartPushData)));
				sPushCallbackRegistered = true;
				return;
			}

			sPushCallbackRegistered = true;
			mEventDispatcher.subscribe(PUSH_OPEN_EVENT, callback);
		}
	}

	@ReactMethod
	public void onPushReceived(Callback callback) {
		synchronized (sStartPushLock) {
			if (!sReceivedPushCallbackRegistered && sReceivedPushData != null) {
				callback.invoke(ConversionUtil.toWritableMap(ConversionUtil.stringToJSONObject(sReceivedPushData)));
				sReceivedPushCallbackRegistered = true;
				return;
			}

			sReceivedPushCallbackRegistered = true;
			mEventDispatcher.subscribe(PUSH_RECEIVED_EVENT, callback);
		}
	}

	@ReactMethod
	public void addListener(String eventName) {
		// Required for NativeEventEmitter. No need to do anything here.
	}

	@ReactMethod
	public void removeListeners(double count) {
		// Required for NativeEventEmitter. No need to do anything here.
	}


	@ReactMethod
	public void setEmails(@NonNull ReadableArray emails, final Callback success, final Callback error) {
		Pushwoosh.getInstance().setEmail(ConversionUtil.messageCodesArrayToArrayList(emails), new com.pushwoosh.function.Callback<Boolean, SetEmailException>() {
			@Override
			public void process(@NonNull Result<Boolean, SetEmailException> result) {
				if (result.isSuccess()) {
					if (success != null) {
						success.invoke();
					}
				} else {
					if (error != null) {
						error.invoke(result.getException().getMessage());
					}
				}
			}
		});
	}

	@ReactMethod
	public void setUserEmails(@NonNull String userId, @NonNull ReadableArray emails, final Callback success, final Callback error) {
		Pushwoosh.getInstance().setUser(userId, ConversionUtil.messageCodesArrayToArrayList(emails), new com.pushwoosh.function.Callback<Boolean, SetUserException>() {
			@Override
			public void process(@NonNull Result<Boolean, SetUserException> result) {
				if (result.isSuccess()) {
					if (success != null) {
						success.invoke();
					}
				} else {
					if (error != null) {
						error.invoke(result.getException().getMessage());
					}
				}
			}
		});
	}

	@ReactMethod
	public void setTags(ReadableMap tags, final Callback success, final Callback error) {
		Pushwoosh.getInstance().sendTags(ConversionUtil.convertToTagsBundle(tags), new com.pushwoosh.function.Callback<Void, PushwooshException>() {
			@Override
			public void process(Result<Void, PushwooshException> result) {
				if (result.isSuccess()) {
					if (success != null) {
						success.invoke();
					}
				} else {
					if (error != null) {
						error.invoke(result.getException().getMessage());
					}
				}
			}
		});
	}

	@ReactMethod
	public void getTags(final Callback success, final Callback error) {
		Pushwoosh.getInstance().getTags(new com.pushwoosh.function.Callback<TagsBundle, GetTagsException>() {
			@Override
			public void process(Result<TagsBundle, GetTagsException> result) {
				if (result.isSuccess()) {
					if (success != null && result.getData() != null) {
						success.invoke(ConversionUtil.toWritableMap(result.getData().toJson()));
					}
				} else {
					if (error != null && result.getException() != null) {
						error.invoke(result.getException().getMessage());
					}
				}
			}
		});
	}

	@ReactMethod
	public void getPushToken(Callback callback) {
		callback.invoke(Pushwoosh.getInstance().getPushToken());
	}

	@ReactMethod
	public void getHwid(Callback callback) {
		callback.invoke(Pushwoosh.getInstance().getHwid());
	}

	@ReactMethod
	public void getUserId(Callback callback) {
		callback.invoke(Pushwoosh.getInstance().getUserId());
	}

	@ReactMethod
	public void setUserId(String userId, final Callback success, final Callback error) {
		Pushwoosh.getInstance().setUserId(userId, new com.pushwoosh.function.Callback<Boolean, SetUserIdException>() {
			@Override
			public void process(Result<Boolean, SetUserIdException> result) {
				if (result.isSuccess()) {
					if (success != null) {
						success.invoke();
					}
				} else {
					if (error != null) {
						error.invoke(result.getException().getMessage());
					}
				}
			}
		});
	}

	@ReactMethod
	public void postEvent(String event, ReadableMap attributes) {
		InAppManager.getInstance().postEvent(event, ConversionUtil.convertToTagsBundle(attributes));
	}

	@ReactMethod
	public void createLocalNotification(ReadableMap data){

		JSONObject params  = ConversionUtil.toJsonObject(data);

		// optString answers "" for a missing key, never null: without a message there is nothing
		// to show, and empty user data has no business reaching the payload.
		String message = params.optString("msg");
		if (message.isEmpty()){
			PWLog.error(TAG, "createLocalNotification: msg is required");
			return;
		}
		int seconds = params.optInt("seconds");
		Bundle extras = new Bundle();
		String userData = params.optString("userData");
		if (!userData.isEmpty()){
		    extras.putString("u", userData);
		}

		LocalNotification notification = new LocalNotification.Builder()
		.setExtras(extras)
		.setMessage(message)
		.setDelay(seconds)
		.build();

		Pushwoosh.getInstance().scheduleLocalNotification(notification);
	}

	@ReactMethod
	public void clearLocalNotification(){
		LocalNotificationReceiver.cancelAll();
	}

	@ReactMethod
	public void clearNotificationCenter() {
		if (AndroidPlatformModule.getManagerProvider() != null) {
			if (AndroidPlatformModule.getManagerProvider().getNotificationManager() != null) {
				AndroidPlatformModule.getManagerProvider().getNotificationManager().cancelAll();
			}
		}
	}

	@ReactMethod
	public void setApplicationIconBadgeNumber(double badgeNumber) {
		PushwooshBadge.setBadgeNumber((int) badgeNumber);
	}

	@ReactMethod
	public void getApplicationIconBadgeNumber(Callback callback) {
		callback.invoke(PushwooshBadge.getBadgeNumber());
	}

	@ReactMethod
	public void addToApplicationIconBadgeNumber(double badgeNumber) {
		PushwooshBadge.addBadgeNumber((int) badgeNumber);
	}

	@ReactMethod
	public void setMultiNotificationMode(boolean on) {
		PushwooshNotificationSettings.setMultiNotificationMode(on);
	}

	@ReactMethod
	public void setLightScreenOnNotification(boolean on) {
		PushwooshNotificationSettings.setLightScreenOnNotification(on);
	}

	@ReactMethod
	public void setEnableLED(boolean on) {
		PushwooshNotificationSettings.setEnableLED(on);
	}

	@ReactMethod
	public void setColorLED(double color) {
		// The spec types a colour as a JS number, so it arrives as a double, and an opaque ARGB
		// value does not fit a signed int: 0xFFFF0000 is 4294901760. A direct double -> int
		// narrowing clamps that to Integer.MAX_VALUE, while going through long keeps the bits the
		// SDK expects (-65536).
		PushwooshNotificationSettings.setColorLED((int) (long) color);
	}

	@ReactMethod
	public void setSoundType(double type) {
		PushwooshNotificationSettings.setSoundNotificationType(SoundType.fromInt((int) type));
	}

	@ReactMethod
	public void setVibrateType(double type) {
		PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.fromInt((int) type));
	}


	@ReactMethod
	public void presentInboxUI(final ReadableMap mapStyle) {
		if (mapStyle != null) {
			inboxUiInboxUiStyleManager.setStyle(mapStyle);
		}

		Activity currentActivity = getCurrentActivity();
		Intent intent = new Intent(currentActivity, InboxActivity.class);
		if (currentActivity != null) {
			currentActivity.startActivity(intent);
		}else {
			PWLog.error(TAG, "current activity is null");
		}
	}

	@ReactMethod
	public void messagesWithNoActionPerformedCount(final Callback callback) {
		PushwooshInbox.messagesWithNoActionPerformedCount(new com.pushwoosh.function.Callback<Integer, InboxMessagesException>() {
			@Override
			public void process(@NonNull Result<Integer, InboxMessagesException> result) {
				if (result.isSuccess() && callback != null) {
					callback.invoke(result.getData());
				}
			}
		});
	}

	@ReactMethod
	public void unreadMessagesCount(final Callback callback) {
		PushwooshInbox.unreadMessagesCount(new com.pushwoosh.function.Callback<Integer, InboxMessagesException>() {
			@Override
			public void process(@NonNull Result<Integer, InboxMessagesException> result) {
				if (result.isSuccess() && callback != null) {
					callback.invoke(result.getData());
				}
			}
		});
	}

	@ReactMethod
	public void messagesCount(final Callback callback) {
		PushwooshInbox.messagesCount(new com.pushwoosh.function.Callback<Integer, InboxMessagesException>() {
			@Override
			public void process(@NonNull Result<Integer, InboxMessagesException> result) {
				if (result.isSuccess() && callback != null) {
					callback.invoke(result.getData());
				}
			}
		});
	}

	@ReactMethod
	public void loadMessages(@NonNull final Callback success, @Nullable final Callback error) {
		PushwooshInbox.loadMessages(new com.pushwoosh.function.Callback<Collection<InboxMessage>, InboxMessagesException>() {
			@Override
			public void process(@NonNull Result<Collection<InboxMessage>, InboxMessagesException> result) {
				try {
					if (result.isSuccess() && result.getData() != null) {
						ArrayList<InboxMessage> messagesList = new ArrayList<>(result.getData());
						WritableArray writableArray = Arguments.createArray();
						for (InboxMessage message : messagesList) {
							writableArray.pushMap(ConversionUtil.inboxMessageToWritableMap(message));
						}
						success.invoke(writableArray);
					} else if (error != null) {
						error.invoke(TAG + "Failed to fetch inbox messages from server");
					}
				} catch (Exception e) {
					if (error != null) {
						error.invoke(e.getLocalizedMessage());
					}
				}
			}
		});
	}

	@ReactMethod
	public void readMessage(String id) {
		PushwooshInbox.readMessage(id);
	}

	@ReactMethod
	public void readMessages(ReadableArray codes) {
		PushwooshInbox.readMessages(ConversionUtil.messageCodesArrayToArrayList(codes));
	}

	@ReactMethod
	public void deleteMessage(String id) {
		PushwooshInbox.deleteMessage(id);
	}

	@ReactMethod
	public void deleteMessages(ReadableArray codes) {
		PushwooshInbox.deleteMessages(ConversionUtil.messageCodesArrayToArrayList(codes));
	}

	@ReactMethod
	public void performAction(String id) {
		PushwooshInbox.performAction(id);
	}

	@ReactMethod
	public void isCommunicationEnabled(final Callback success){
		success.invoke(Pushwoosh.getInstance().isServerCommunicationAllowed());
	}

	@ReactMethod
	public void setCommunicationEnabled(boolean enable, final Callback success, final Callback error) {
		try {
			if (enable) {
				Pushwoosh.getInstance().startServerCommunication();
			} else {
				Pushwoosh.getInstance().stopServerCommunication();
			}
			success.invoke();
		} catch (Exception e) {
			if (error != null) {
				error.invoke(e.getMessage());
			}
		}
	}


	@ReactMethod
	public void setLanguage(String language){
		Pushwoosh.getInstance().setLanguage(language);
	}

	@ReactMethod
	public void setNotificationIconBackgroundColor(String color) {
		int intColor;
		try {
            intColor = Color.parseColor(color);
			PushwooshNotificationSettings.setNotificationIconBackgroundColor(intColor);
        } catch (IllegalArgumentException e) {
            PWLog.exception(e);
        }
 	}

	 @ReactMethod
	 public void enableHuaweiPushNotifications() {
		 Pushwoosh.getInstance().enableHuaweiPushNotifications();
	 }

	@ReactMethod
	public void registerSMSNumber(String phoneNumber) {
		Pushwoosh.getInstance().registerSMSNumber(phoneNumber);
	}

	@ReactMethod
	public void registerWhatsappNumber(String phoneNumber) {
		Pushwoosh.getInstance().registerWhatsappNumber(phoneNumber);
	}

	@ReactMethod
	public void setRichMediaType(double type) {
		RichMediaType richMediaType = (int) type == 0 ? RichMediaType.MODAL : RichMediaType.DEFAULT;
		RichMediaManager.setRichMediaType(richMediaType);
	}

	@ReactMethod
	public void getRichMediaType(Callback callback) {
		RichMediaType type = RichMediaManager.getRichMediaType();
		callback.invoke(type.ordinal());
	}

	// iOS-only setting. Android shows a notification for a push that arrives in the foreground,
	// which is what the iOS default reports; the methods exist so the spec is fully implemented.
	@ReactMethod
	public void setShowPushnotificationAlert(boolean showPushnotificationAlert) {
	}

	@ReactMethod
	public void getShowPushnotificationAlert(Callback callback) {
		callback.invoke(true);
	}

	///
	/// LifecycleEventListener callbacks
	///

	@Override
	public void onHostResume() {
		PWLog.noise(TAG, "Host resumed");
	}

	@Override
	public void onHostPause() {
		PWLog.noise(TAG, "Host paused");
	}

	@Override
	public void onHostDestroy() {
		PWLog.noise(TAG, "Host destroyed");

		// A push can be delivered on the FCM thread while the host is going down, and every other
		// accessor of these fields holds the lock.
		synchronized (sStartPushLock) {
			// Only for the module this host is taking down. A restarted host builds a new
			// PushwooshPlugin, and the order of the two events is not guaranteed, so a late
			// teardown must not reset what the new module has already set up.
			if (INSTANCE != this) {
				return;
			}

			sPushCallbackRegistered = false;
			sStartPushData = null;

			sReceivedPushCallbackRegistered = false;
			sReceivedPushData = null;

			// These are what openPush()/messageReceived() consult before sending to JS. Left
			// standing, a push arriving after the host died was handed to a context with no JS
			// runtime, the broad catch there swallowed the failure, and the push was gone for
			// good - the cache that would have replayed it on the next init() had just been
			// cleared above. Cleared, the same push is cached and the next init() replays it.
			sInitialized = false;
			INSTANCE = null;
		}
	}

	///
	/// Private methods
	///

	static void openPush(String pushData) {
		PWLog.info(TAG, "Push open: " + pushData);

		try {
			synchronized (sStartPushLock) {
				sStartPushData = pushData;
				if (sPushCallbackRegistered) {
					mEventDispatcher.dispatchEvent(PUSH_OPEN_EVENT, ConversionUtil.toWritableMap(ConversionUtil.stringToJSONObject(pushData)));
				}
				if (sInitialized && INSTANCE != null) {
					INSTANCE.sendEvent(PUSH_OPEN_JS_EVENT, ConversionUtil.stringToJSONObject(pushData));
				}
			}
		} catch (Exception e) {
			// React Native is highly unstable
			PWLog.exception(e);
		}
	}

	private void sendEvent(String event, JSONObject params) {
		mEventDispatcher.sendJSEvent(getReactApplicationContext(), event, ConversionUtil.toWritableMap(params));
	}

	static void messageReceived(String pushData) {
		PWLog.info(TAG, "Push received: " + pushData);

		try {
			synchronized (sStartPushLock) {
				sReceivedPushData = pushData;
				if (sReceivedPushCallbackRegistered) {
					mEventDispatcher.dispatchEvent(PUSH_RECEIVED_EVENT, ConversionUtil.toWritableMap(ConversionUtil.stringToJSONObject(pushData)));
				}
				if (sInitialized && INSTANCE != null) {
					INSTANCE.sendEvent(PUSH_RECEIVED_JS_EVENT, ConversionUtil.stringToJSONObject(pushData));
				}
			}
		} catch (Exception e) {
			// React Native is highly unstable
			PWLog.exception(e);
		}
	}

	private class RegisterForPushNotificationCallback implements com.pushwoosh.function.Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> {
		private Callback success;
		private Callback error;

		public RegisterForPushNotificationCallback(Callback success, Callback error) {
			this.success = success;
			this.error = error;
		}

		@Override
		public void process(Result<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> result) {
			if (result.isSuccess()) {
				if (success != null && result.getData() != null) {
					success.invoke(result.getData().getToken());
					success = null;
				}
			} else if (result.getException() != null) {
				if (error != null) {
					error.invoke(result.getException().getLocalizedMessage());
					error = null;
				}
			}
		}
	}
}
