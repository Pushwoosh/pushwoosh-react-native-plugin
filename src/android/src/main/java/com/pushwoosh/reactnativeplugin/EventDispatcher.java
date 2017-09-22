package com.pushwoosh.reactnativeplugin;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class EventDispatcher {

    private  Map<String, List<Callback>> subscribers = new HashMap<String, List<Callback>>();

    public void subscribe(String event, Callback callback) {
        if (callback == null) {
            return;
        }

        synchronized (subscribers) {
            if (!subscribers.containsKey(event)) {
                subscribers.put(event, new ArrayList<Callback>());
            }

            List<Callback> list = subscribers.get(event);
            list.add(callback);
        }
    }

    public void dispatchEvent(String event, Object... objects) {
        synchronized (subscribers) {
            if (!subscribers.containsKey(event)) {
                return;
            }

            List<Callback> list = subscribers.get(event);
            for (Callback subscriber : list) {
                subscriber.invoke(objects);
            }

            // A native module is supposed to invoke its callback only once!
            list.clear();
        }
    }

    public void sendJSEvent(ReactContext reactContext, String event, WritableMap params) {
        reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(event, params);
    }
}
