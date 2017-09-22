package com.pushwoosh.reactnativeplugin;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.pushwoosh.Pushwoosh;
import com.pushwoosh.badge.PushwooshBadge;
import com.pushwoosh.exception.GetTagsException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Result;
import com.pushwoosh.inapp.PushwooshInApp;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.location.PushwooshLocation;
import com.pushwoosh.notification.PushwooshNotificationSettings;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONObject;

public class PushwooshPlugin extends ReactContextBaseJavaModule implements LifecycleEventListener {

	static final String TAG = "ReactNativePlugin";
	private static final String PUSH_OPEN_JS_EVENT = "pushOpened";

	private static EventDispatcher mEventDispatcher = new EventDispatcher();

	private boolean mBroadcastPush = true;

	private static String mStartPushData;
	private static final Object mStartPushLock = new Object();
	private static boolean mPushCallbackRegistered = false;
	private static boolean mInitialized = false;

	private static PushwooshPlugin INSTANCE = null;

	public PushwooshPlugin(ReactApplicationContext reactContext) {
		super(reactContext);

		INSTANCE = this;

		try {
			String packageName = reactContext.getPackageName();
			ApplicationInfo ai = reactContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);

			if (ai.metaData != null) {
				mBroadcastPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", true);
			}
		} catch (Exception e) {
			PWLog.exception(e);
		}

		PWLog.debug(TAG, "broadcastPush = " + mBroadcastPush);

		reactContext.addLifecycleEventListener(this);
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

		Pushwoosh.getInstance().setAppId(appId);
		Pushwoosh.getInstance().setSenderId(projectId);

		synchronized (mStartPushLock) {
			if (mStartPushData != null) {
				sendEvent(PUSH_OPEN_JS_EVENT, ConversionUtil.stringToJSONObject(mStartPushData));
			}
		}

		mInitialized = true;

		if (success != null) {
			success.invoke();
		}
	}

	@ReactMethod
	public void register(final Callback success, final Callback error) {
		Pushwoosh.getInstance().registerForPushNotifications(new com.pushwoosh.function.Callback<String, RegisterForPushNotificationsException>() {
			@Override
			public void process(@NonNull Result<String, RegisterForPushNotificationsException> result) {
				if (result.isSuccess()) {
					success.invoke(result.getData());
				} else if (result.getException() != null) {
					error.invoke(result.getException().getLocalizedMessage());
				}
			}
		});
	}

	@ReactMethod
	public void unregister(final Callback success, final Callback error) {
		Pushwoosh.getInstance().unregisterForPushNotifications(new com.pushwoosh.function.Callback<String, UnregisterForPushNotificationException>() {
			@Override
			public void process(@NonNull Result<String, UnregisterForPushNotificationException> result) {
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
		synchronized (mStartPushLock) {
			if (!mPushCallbackRegistered && mStartPushData != null) {
				callback.invoke(ConversionUtil.toWritableMap(ConversionUtil.stringToJSONObject(mStartPushData)));
				mPushCallbackRegistered = true;
				return;
			}

			mPushCallbackRegistered = true;
			mEventDispatcher.subscribe(Pushwoosh.PUSH_RECEIVE_EVENT, callback);
		}
	}

	@ReactMethod
	public void setTags(ReadableMap tags, final Callback success, final Callback error) {
		Pushwoosh.getInstance().sendTags(ConversionUtil.convertToTagsBundle(tags), new com.pushwoosh.function.Callback<Void, PushwooshException>() {
			@Override
			public void process(@NonNull Result<Void, PushwooshException> result) {
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
			public void process(@NonNull Result<TagsBundle, GetTagsException> result) {
				if(result.isSuccess()){
					if (success != null && result.getData()!=null) {
						success.invoke(ConversionUtil.toWritableMap(result.getData().toJson()));
					}
				} else {
					if (error != null && result.getException()!=null) {
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
	public void setUserId(String userId) {
		PushwooshInApp.getInstance().setUserId(userId);
	}

	@ReactMethod
	public void postEvent(String event, ReadableMap attributes) {
		PushwooshInApp.getInstance().postEvent(event, ConversionUtil.convertToTagsBundle(attributes));
	}

	@ReactMethod
	public void startLocationTracking() {
		PushwooshLocation.startLocationTracking();
	}

	@ReactMethod
	public void stopLocationTracking() {
		PushwooshLocation.stopLocationTracking();
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
		PushwooshNotificationSettings.setColorLED( color);
	}

	@ReactMethod
	public void setSoundType(int type) {
		PushwooshNotificationSettings.setSoundNotificationType(SoundType.fromInt(type));
	}

	@ReactMethod
	public void setVibrateType(int type) {
		PushwooshNotificationSettings.setVibrateNotificationType(VibrateType.fromInt(type));
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

		mPushCallbackRegistered = false;
		mStartPushData = null;
	}

	///
	/// Private methods
	///

	static void openPush(String pushData) {
		PWLog.info(TAG, "Push open: " + pushData);

		try {
			synchronized (mStartPushLock) {
				mStartPushData = pushData;
				if (mPushCallbackRegistered) {
					mEventDispatcher.dispatchEvent(Pushwoosh.PUSH_RECEIVE_EVENT, ConversionUtil.toWritableMap(ConversionUtil.stringToJSONObject(pushData)));
				}
				if (mInitialized && INSTANCE != null) {
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
}
