package com.pushwoosh.reactnativeplugin;

import android.app.Activity;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.pushwoosh.GDPRManager;
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
import com.pushwoosh.inapp.PushwooshInApp;
import com.pushwoosh.inbox.PushwooshInbox;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity;
import com.pushwoosh.internal.network.NetworkModule;
import com.pushwoosh.internal.platform.AndroidPlatformModule;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.PushwooshNotificationSettings;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.tags.TagsBundle;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.notification.LocalNotificationReceiver;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;

public class PushwooshPlugin extends ReactContextBaseJavaModule implements LifecycleEventListener {

	static final String TAG = "ReactNativePlugin";
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
		return "Pushwoosh";
	}


	@ReactMethod
	public void init(ReadableMap config, Callback success, Callback error) {
		String appId = config.getString("pw_appid");
		String projectId = config.getString("project_number");

		if (appId == null || projectId == null) {
			if (error != null) {
				error.invoke("Pushwoosh Application id and GCM project number not specified");
			}
			return;
		}

		if (config.hasKey("reverse_proxy_url")) {
			String proxyUrl = config.getString("reverse_proxy_url");
			if (!TextUtils.isEmpty(proxyUrl) && NetworkModule.getRequestManager() != null) {
				NetworkModule.getRequestManager().setReverseProxyUrl(proxyUrl);
			}
		}

		Pushwoosh.getInstance().setAppId(appId);
		
		if (!TextUtils.isEmpty(projectId)) {
			Pushwoosh.getInstance().setSenderId(projectId);
		}

		synchronized (sStartPushLock) {
			if (sReceivedPushData != null) {
				sendEvent(PUSH_RECEIVED_JS_EVENT, ConversionUtil.stringToJSONObject(sReceivedPushData));
			}

			if (sStartPushData != null) {
				sendEvent(PUSH_OPEN_JS_EVENT, ConversionUtil.stringToJSONObject(sStartPushData));
			}
		}

		sInitialized = true;

		if (success != null) {
			success.invoke();
		}
	}

	@ReactMethod
	public void register(final Callback success, final Callback error) {
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
		PushwooshInApp.getInstance().postEvent(event, ConversionUtil.convertToTagsBundle(attributes));
	}

	@ReactMethod
	public void createLocalNotification(ReadableMap data){

		JSONObject params  = ConversionUtil.toJsonObject(data);

		String message = params.optString("msg");
		if (message == null){
			return;
		}
		int seconds = params.optInt("seconds");
		Bundle extras = new Bundle();
		String userData = params.optString("userData");
		if (userData!=null){
		    extras.putString("u", userData);
		}

		LocalNotification.Builder builder = new LocalNotification.Builder();
		if (extras != null){
			builder.setExtras(extras);
		}
		LocalNotification notification = builder
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
	public void setApplicationIconBadgeNumber(int badgeNumber) {
		PushwooshBadge.setBadgeNumber(badgeNumber);
	}

	@ReactMethod
	public void getApplicationIconBadgeNumber(Callback callback) {
		callback.invoke(PushwooshBadge.getBadgeNumber());
	}

	@ReactMethod
	public void addToApplicationIconBadgeNumber(int badgeNumber) {
		PushwooshBadge.addBadgeNumber(badgeNumber);
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
	public void setColorLED(int color) {
		PushwooshNotificationSettings.setColorLED(color);
	}

	@ReactMethod
	public void setSoundType(int type) {
		PushwooshNotificationSettings.setSoundNotificationType(SoundType.fromInt(type));
	}

	@ReactMethod
	public void setVibrateType(int type) {
		PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.fromInt(type));
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
						WritableNativeArray writableArray = new WritableNativeArray();
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
	public void showGDPRConsentUI(){
		GDPRManager.getInstance().showGDPRConsentUI();
	}

	@ReactMethod
	public void showGDPRDeletionUI(){
		GDPRManager.getInstance().showGDPRDeletionUI();
	}

	@ReactMethod
	public void isDeviceDataRemoved(final Callback success){
		success.invoke(GDPRManager.getInstance().isDeviceDataRemoved());
	}

	@ReactMethod
	public void isCommunicationEnabled(final Callback success){
		success.invoke(GDPRManager.getInstance().isCommunicationEnabled());
	}

	@ReactMethod
	public void isAvailableGDPR(final Callback success){
		success.invoke(GDPRManager.getInstance().isAvailable());
	}

	@ReactMethod
	public void setCommunicationEnabled(boolean enable, final Callback success, final Callback error) {
		GDPRManager.getInstance().setCommunicationEnabled(enable, new com.pushwoosh.function.Callback<Void, PushwooshException>() {
			@Override
			public void process(Result<Void, PushwooshException> result) {
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
	public void removeAllDeviceData(final Callback success, final Callback error) {
		GDPRManager.getInstance().removeAllDeviceData(new com.pushwoosh.function.Callback<Void, PushwooshException>() {
			@Override
			public void process(Result<Void, PushwooshException> result) {
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

		sPushCallbackRegistered = false;
		sStartPushData = null;

		sReceivedPushCallbackRegistered = false;
		sReceivedPushData = null;
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
