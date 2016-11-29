package com.pushwoosh.reactnativeplugin;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.pushwoosh.BasePushMessageReceiver;
import com.pushwoosh.BaseRegistrationReceiver;
import com.pushwoosh.PushManager;
import com.pushwoosh.SendPushTagsCallBack;
import com.pushwoosh.inapp.InAppFacade;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.notification.SoundType;
import com.pushwoosh.notification.VibrateType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PushwooshPlugin extends ReactContextBaseJavaModule implements LifecycleEventListener {

    static final String TAG = "ReactNativePlugin";

    static final String PUSH_OPEN_JS_EVENT = "pushOpened";

    private PushManager mPushManager;

    private static EventDispatcher mEventDispatcher = new EventDispatcher();

    private boolean mBroadcastPush = true;

    private static String mStartPushData;
    private static Object mStartPushLock = new Object();
    private static boolean mPushCallbackRegistered = false;
    private static boolean mInitialized = false;

    private static PushwooshPlugin INSTANCE = null;

    BroadcastReceiver mRegistationReceiver = new BaseRegistrationReceiver() {
        @Override
        protected void onRegisterActionReceive(Context context, Intent intent) {
            if (intent == null) {
                return;
            }

            else if (intent.hasExtra(PushManager.REGISTER_EVENT)) {
                String pushToken = intent.getExtras().getString(PushManager.REGISTER_EVENT);
                mEventDispatcher.dispatchEvent(PushManager.REGISTER_EVENT, pushToken);
            }
            else if (intent.hasExtra(PushManager.UNREGISTER_EVENT)) {
                String pushToken = intent.getExtras().getString(PushManager.UNREGISTER_EVENT);
                mEventDispatcher.dispatchEvent(PushManager.UNREGISTER_EVENT, pushToken);
            }
            else if (intent.hasExtra(PushManager.REGISTER_ERROR_EVENT)) {
                String errorMessage = intent.getExtras().getString(PushManager.REGISTER_ERROR_EVENT);
                mEventDispatcher.dispatchEvent(PushManager.REGISTER_ERROR_EVENT, errorMessage);
            }
            else if (intent.hasExtra(PushManager.UNREGISTER_ERROR_EVENT)) {
                String errorMessage = intent.getExtras().getString(PushManager.UNREGISTER_ERROR_EVENT);
                mEventDispatcher.dispatchEvent(PushManager.UNREGISTER_ERROR_EVENT, errorMessage);
            }
        }
    };

    BroadcastReceiver mPushMessageReceiver = new BasePushMessageReceiver() {
        @Override
        protected void onMessageReceive(Intent intent) {
            if (intent == null) {
                return;
            }

            String pushData = intent.getStringExtra(JSON_DATA_KEY);
            openPush(pushData);
        }
    };

    public PushwooshPlugin(ReactApplicationContext reactContext) {
        super(reactContext);

        INSTANCE = this;

        try {
            String packageName = reactContext.getPackageName();
            ApplicationInfo ai = reactContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);

            if (ai.metaData != null) {
                mBroadcastPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", true);
            }
        }
        catch (Exception e) {
            PWLog.exception(e);
        }

        PWLog.debug(TAG, "broadcastPush = " + mBroadcastPush);

        registerPushReceiver();

        reactContext.addLifecycleEventListener(this);
    }

    public static void openPush(String pushData) {
        PWLog.info(TAG, "Push open: " + pushData);

        try {
            synchronized (mStartPushLock) {
                mStartPushData = pushData;
                if (mPushCallbackRegistered) {
                    mEventDispatcher.dispatchEvent(PushManager.PUSH_RECEIVE_EVENT, ConversionUtil.toWritableMap(ConversionUtil.stringToMap(pushData)));
                }
                if (mInitialized && INSTANCE != null) {
                    INSTANCE.sendEvent(PUSH_OPEN_JS_EVENT, ConversionUtil.stringToMap(pushData));
                }
            }
        }
        catch (Exception e) {
            // React Native is highly unstable
            PWLog.exception(e);
        }
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

        Context context = getReactApplicationContext();

        context.registerReceiver(mRegistationReceiver, new IntentFilter(context.getPackageName() + "." + PushManager.REGISTER_BROAD_CAST_ACTION));

        PushManager.initializePushManager(context, appId, projectId);

        mPushManager = PushManager.getInstance(context);

        try {
            mPushManager.onStartup(context);
        }
        catch (Exception e) {
            PWLog.exception(e);
            if (error != null) {
                error.invoke(e.getMessage());
            }
            return;
        }

        synchronized (mStartPushLock) {
            if (mStartPushData != null) {
                sendEvent(PUSH_OPEN_JS_EVENT, ConversionUtil.stringToMap(mStartPushData));
            }
        }

        mInitialized = true;

        if (success != null) {
            success.invoke();
        }
    }

    @ReactMethod
    public void register(Callback success, Callback error) {
        mEventDispatcher.subscribe(PushManager.REGISTER_EVENT, success);
        mEventDispatcher.subscribe(PushManager.REGISTER_ERROR_EVENT, error);
        mPushManager.registerForPushNotifications();
    }

    @ReactMethod
    public void unregister(Callback success, Callback error) {
        mEventDispatcher.subscribe(PushManager.UNREGISTER_EVENT, success);
        mEventDispatcher.subscribe(PushManager.UNREGISTER_ERROR_EVENT, error);
        mPushManager.unregisterForPushNotifications();
    }

    @ReactMethod
    public void onPushOpen(Callback callback) {
        synchronized (mStartPushLock) {
            if (!mPushCallbackRegistered && mStartPushData != null) {
                callback.invoke(ConversionUtil.toWritableMap(ConversionUtil.stringToMap(mStartPushData)));
                mPushCallbackRegistered = true;
                return;
            }

            mPushCallbackRegistered = true;
            mEventDispatcher.subscribe(PushManager.PUSH_RECEIVE_EVENT, callback);
        }
    }

    @ReactMethod
    public void setTags(ReadableMap tags, final Callback success, final Callback error) {
        mPushManager.sendTags(getReactApplicationContext(), ConversionUtil.toMap(tags), new SendPushTagsCallBack() {
            @Override
            public void taskStarted() {

            }

            @Override
            public void onSentTagsSuccess(Map<String, String> result) {
                if (success != null) {
                    success.invoke();
                }
            }

            @Override
            public void onSentTagsError(Exception e) {
                if (error != null) {
                    error.invoke(e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void getTags(final Callback success, final Callback error) {
        mPushManager.getTagsAsync(getReactApplicationContext(), new PushManager.GetTagsListener() {
            @Override
            public void onTagsReceived(Map<String, Object> tags) {
                if (success != null) {
                    success.invoke(ConversionUtil.toWritableMap(tags));
                }
            }

            @Override
            public void onError(Exception e) {
                if (error != null) {
                    error.invoke(e.getMessage());
                }
            }
        });
    }

    @ReactMethod
    public void getPushToken(Callback callback) {
        callback.invoke(mPushManager.getPushToken(getReactApplicationContext()));
    }

    @ReactMethod
    public void getHwid(Callback callback) {
        callback.invoke(mPushManager.getPushwooshHWID(getReactApplicationContext()));
    }

    @ReactMethod
    public void setUserId(String userId) {
        mPushManager.setUserId(getReactApplicationContext(), userId);
    }

    @ReactMethod
    public void postEvent(String event, ReadableMap attributes) {
        Intent intent = PostEventActivity.createIntent(getReactApplicationContext(), event, ConversionUtil.toMap(attributes));
        getReactApplicationContext().startActivity(intent);
    }

    @ReactMethod
    public void startLocationTracking() {
        mPushManager.startTrackingGeoPushes();
    }

    @ReactMethod
    public void stopLocationTracking() {
        mPushManager.stopTrackingGeoPushes();
    }

    @ReactMethod
    public void setApplicationIconBadgeNumber(int badgeNumber) {
        mPushManager.setBadgeNumber(badgeNumber);
    }

    @ReactMethod
    public void getApplicationIconBadgeNumber(Callback callback) {
        callback.invoke(mPushManager.getBadgeNumber());
    }

    @ReactMethod
    public void addToApplicationIconBadgeNumber(int badgeNumber) {
        mPushManager.addBadgeNumber(badgeNumber);
    }

    @ReactMethod
    public void setMultiNotificationMode(boolean on) {
        Context context = getReactApplicationContext();
        if (on) {
            PushManager.setMultiNotificationMode(context);
        }
        else {
            PushManager.setSimpleNotificationMode(context);
        }
    }

    @ReactMethod
    public void setLightScreenOnNotification(boolean on) {
        Context context = getReactApplicationContext();
        PushManager.setLightScreenOnNotification(context, on);
    }

    @ReactMethod
    public void setEnableLED(boolean on) {
        Context context = getReactApplicationContext();
        PushManager.setEnableLED(context, on);
    }

    @ReactMethod
    public void setColorLED(int color) {
        Context context = getReactApplicationContext();
        PushManager.setColorLED(context, color);
    }

    @ReactMethod
    public void setSoundType(int type) {
        Context context = getReactApplicationContext();
        PushManager.setSoundNotificationType(context, SoundType.fromInt(type));
    }

    @ReactMethod
    public void setVibrateType(int type) {
        Context context = getReactApplicationContext();
        PushManager.setVibrateNotificationType(context, VibrateType.fromInt(type));
    }

    ///
    /// LifecycleEventListener callbacks
    ///

    @Override
    public void onHostResume() {
        PWLog.noise(TAG, "Host resumed");

        registerPushReceiver();
    }

    @Override
    public void onHostPause() {
        PWLog.noise(TAG, "Host paused");

        unregisterPushReceiver();
    }

    @Override
    public void onHostDestroy() {
        PWLog.noise(TAG, "Host destroyed");

        try {
            Context context = getReactApplicationContext();
            context.unregisterReceiver(mRegistationReceiver);
        }
        catch (Exception e) {

        }

        mPushCallbackRegistered = false;
        mStartPushData = null;
    }

    ///
    /// Private methods
    ///

    private void registerPushReceiver() {
        if (!mBroadcastPush) {
            return;
        }

        try {
            Context context = getReactApplicationContext();
            IntentFilter intentFilter = new IntentFilter(context.getPackageName() + ".action.PUSH_MESSAGE_RECEIVE");
            context.registerReceiver(mPushMessageReceiver, intentFilter);
        }
        catch (Exception e) {

        }
    }

    private void unregisterPushReceiver() {
        if (!mBroadcastPush) {
            return;
        }

        try {
            Context context = getReactApplicationContext();
            context.unregisterReceiver(mPushMessageReceiver);
        }
        catch (Exception e) {

        }
    }

    private void sendEvent(String event, Map<String, Object> params) {
        mEventDispatcher.sendJSEvent(getReactApplicationContext(), event, params);
    }
}
