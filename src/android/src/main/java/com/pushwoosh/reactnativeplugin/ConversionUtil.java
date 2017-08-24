package com.pushwoosh.reactnativeplugin;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.pushwoosh.internal.utils.JsonUtils;
import com.pushwoosh.internal.utils.PWLog;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.R.attr.key;

public final class ConversionUtil {

    public static Map<String, Object> toMap(ReadableMap readableMap) {
        ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
        Map<String, Object> result = new HashMap<>();
        while (iterator.hasNextKey()) {
            String key = iterator.nextKey();
            ReadableType type = readableMap.getType(key);
            switch (type) {
                case Null:
                    result.put(key, null);
                    break;
                case Boolean:
                    result.put(key, readableMap.getBoolean(key));
                    break;
                case Number:
                    result.put(key, readableMap.getDouble(key));
                    break;
                case String:
                    result.put(key, readableMap.getString(key));
                    break;
                case Map:
                    result.put(key, toMap(readableMap.getMap(key)));
                    break;
                case Array:
                    result.put(key, toArray(readableMap.getArray(key)));
                    break;
                default:
                    PWLog.error(PushwooshPlugin.TAG, "Could not convert object with key: " + key + ".");
            }

        }
        return result;
    }

    public static List<Object> toArray(ReadableArray readableArray) {
        List<Object> result = new ArrayList<>(readableArray.size());
        for (int i = 0; i < readableArray.size(); i++) {
            ReadableType indexType = readableArray.getType(i);
            switch(indexType) {
                case Null:
                    result.add(i, null);
                    break;
                case Boolean:
                    result.add(i, readableArray.getBoolean(i));
                    break;
                case Number:
                    result.add(i, readableArray.getDouble(i));
                    break;
                case String:
                    result.add(i, readableArray.getString(i));
                    break;
                case Map:
                    result.add(i, toMap(readableArray.getMap(i)));
                    break;
                case Array:
                    result.add(i, toArray(readableArray.getArray(i)));
                    break;
                default:
                    PWLog.error(PushwooshPlugin.TAG, "Could not convert object at index " + i + ".");
            }
        }
        return result;
    }

    public static Map<String, Object> stringToMap(String string) {
        try {
            JSONObject json = new JSONObject(string);
            return JsonUtils.jsonToMap(json);
        }
        catch (JSONException e) {
            PWLog.exception(e);
        }

        return Collections.<String, Object>emptyMap();
    }

    public static WritableMap toWritableMap(Map<String, Object> map) {
        WritableNativeMap result = new WritableNativeMap();

        for(Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value == null) {
                result.putNull(key);
            }
            else if (value instanceof Map) {
                result.putMap(key, toWritableMap((Map)value));
            }
            else if (value instanceof List) {
                result.putArray(key, toWritableArray((List) value));
            }
            else if (value instanceof Boolean) {
                result.putBoolean(key, (Boolean)value);
            }
            else if (value instanceof Integer) {
                result.putInt(key, (Integer)value);
            }
            else if(value instanceof Long){
                result.putString(key, String.valueOf(value));
            }
            else if (value instanceof String) {
                result.putString(key, (String)value);
            }
            else if (value instanceof Double) {
                result.putDouble(key, (Double)value);
            }
            else {
                PWLog.error(PushwooshPlugin.TAG, "Could not convert object " + value.toString());
            }
        }

        return result;
    }

    public static WritableArray toWritableArray(List<Object> array) {
        WritableNativeArray result = new WritableNativeArray();

        for (Object value : array) {
            if (value == null) {
                result.pushNull();
            }
            else if (value instanceof Map) {
                result.pushMap(toWritableMap((Map) value));
            }
            else if (value instanceof List) {
                result.pushArray(toWritableArray((List) value));
            }
            else if (value instanceof Boolean) {
                result.pushBoolean((Boolean) value);
            }
            else if (value instanceof Integer) {
                result.pushInt((Integer) value);
            }
            else if (value instanceof String) {
                result.pushString((String) value);
            }
            else if (value instanceof Double) {
                result.pushDouble((Double) value);
            }
            else {
                PWLog.error(PushwooshPlugin.TAG, "Could not convert object " + value.toString());
            }
        }

        return result;
    }
}