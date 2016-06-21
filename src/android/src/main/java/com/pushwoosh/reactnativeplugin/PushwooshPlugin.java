package com.pushwoosh.reactnativeplugin;

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
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class PushwooshPlugin extends ReactContextBaseJavaModule implements LifecycleEventListener {

    static final String TAG = "ReactNativePlugin";

    private PushManager mPushManager;

    private static EventDispatcher mEventDispatcher = new EventDispatcher();

    private boolean mBroadcastPush = true;

    private static String mStartPushData;
    private static Object mStartPushLock = new Object();
    private static boolean mPushCallbackRegistered = false;

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

        try {
            String packageName = reactContext.getPackageName();
            ApplicationInfo ai = reactContext.getPackageManager().getApplicationInfo(packageName, PackageManager.GET_META_DATA);

            if (ai.metaData != null) {
                mBroadcastPush = ai.metaData.getBoolean("PW_BROADCAST_PUSH", true);
            }
        }
        catch (Exception e) {
            Log.exception(e);
        }

        Log.debug(TAG, "broadcastPush = " + mBroadcastPush);

        registerPushReceiver();

        reactContext.addLifecycleEventListener(this);
    }

    public static void openPush(String pushData) {
        Log.info(TAG, "Push open: " + pushData);

        try {
            synchronized (mStartPushLock) {
                mStartPushData = pushData;
                if (mPushCallbackRegistered) {
                    mEventDispatcher.dispatchEvent(PushManager.PUSH_RECEIVE_EVENT, ConversionUtil.toWritableMap(ConversionUtil.stringToMap(pushData)));
                }
            }
        }
        catch (Exception e) {
            // React Native is highly unstable
            Log.exception(e);
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
            Log.exception(e);
            if (error != null) {
                error.invoke(e.getMessage());
            }
            return;
        }

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

    ///
    /// LifecycleEventListener callbacks
    ///

    @Override
    public void onHostResume() {
        Log.noise(TAG, "Host resumed");

        registerPushReceiver();
    }

    @Override
    public void onHostPause() {
        Log.noise(TAG, "Host paused");

        unregisterPushReceiver();
    }

    @Override
    public void onHostDestroy() {

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
}
