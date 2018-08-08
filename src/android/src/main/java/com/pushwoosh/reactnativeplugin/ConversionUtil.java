package com.pushwoosh.reactnativeplugin;

import java.util.Iterator;

import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.ReadableType;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeArray;
import com.facebook.react.bridge.WritableNativeMap;
import com.pushwoosh.internal.utils.PWLog;
import com.pushwoosh.tags.TagsBundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public final class ConversionUtil {

	public static TagsBundle convertToTagsBundle(ReadableMap readableMap) {
		return new TagsBundle.Builder()
				.putAll(toJsonObject(readableMap))
				.build();
	}

	public static JSONObject toJsonObject(ReadableMap readableMap) {
		ReadableMapKeySetIterator iterator = readableMap.keySetIterator();
		JSONObject result = new JSONObject();
		while (iterator.hasNextKey()) {
			String key = iterator.nextKey();
			ReadableType type = readableMap.getType(key);
			try {
				switch (type) {
					case Null:
						result.put(key, JSONObject.NULL);
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
						result.put(key, toJsonObject(readableMap.getMap(key)));
						break;
					case Array:
						result.put(key, toArray(readableMap.getArray(key)));
						break;
					default:
						PWLog.error(PushwooshPlugin.TAG, "Could not convert object with key: " + key + ".");
				}

			} catch (JSONException e) {
				PWLog.error(PushwooshPlugin.TAG, "Could not convert object with key: " + key + ".", e);
			}

		}
		return result;
	}

	public static JSONArray toArray(ReadableArray readableArray) {
		JSONArray result = new JSONArray();
		for (int i = 0; i < readableArray.size(); i++) {
			ReadableType indexType = readableArray.getType(i);
			try {
				switch (indexType) {
					case Null:
						result.put(i, JSONObject.NULL);
						break;
					case Boolean:
						result.put(i, readableArray.getBoolean(i));
						break;
					case Number:
						result.put(i, readableArray.getDouble(i));
						break;
					case String:
						result.put(i, readableArray.getString(i));
						break;
					case Map:
						result.put(i, toJsonObject(readableArray.getMap(i)));
						break;
					case Array:
						result.put(i, toArray(readableArray.getArray(i)));
						break;
					default:
						PWLog.error(PushwooshPlugin.TAG, "Could not convert object at index " + i + ".");
				}
			} catch (JSONException e) {
				PWLog.error(PushwooshPlugin.TAG, "Could not convert object at index " + i + ".", e);
			}
		}
		return result;
	}

	public static JSONObject stringToJSONObject(String string) {
		try {
			JSONObject json = new JSONObject(string);
			return json;
		} catch (JSONException e) {
			PWLog.exception(e);
		}

		return new JSONObject();
	}

	public static WritableMap toWritableMap(JSONObject jsonObject) {
		WritableNativeMap result = new WritableNativeMap();

		Iterator<String> keys = jsonObject.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			try {
				Object value = jsonObject.get(key);

				if (value == null || JSONObject.NULL.equals(value)) {
					result.putNull(key);
				} else if (value instanceof JSONObject) {
					result.putMap(key, toWritableMap((JSONObject) value));
				} else if (value instanceof JSONArray) {
					result.putArray(key, toWritableArray((JSONArray) value));
				} else if (value instanceof Boolean) {
					result.putBoolean(key, (Boolean) value);
				} else if (value instanceof Integer) {
					result.putInt(key, (Integer) value);
				} else if (value instanceof Long) {
					result.putString(key, String.valueOf(value));
				} else if (value instanceof String) {
					result.putString(key, (String) value);
				} else if (value instanceof Double) {
					result.putDouble(key, (Double) value);
				} else {
					PWLog.error(PushwooshPlugin.TAG, "Could not convert object " + value.toString());
				}

			} catch (JSONException e) {
				PWLog.error(PushwooshPlugin.TAG, "Could not convert object with key " + key, e);
			}
		}

		return result;
	}

	public static WritableArray toWritableArray(JSONArray array) {
		WritableNativeArray result = new WritableNativeArray();

		for (int i = 0; i < array.length(); i++) {
			try {
				Object value = array.get(i);
				if (value == null) {
					result.pushNull();
				} else if (value instanceof JSONObject) {
					result.pushMap(toWritableMap((JSONObject) value));
				} else if (value instanceof JSONArray) {
					result.pushArray(toWritableArray((JSONArray) value));
				} else if (value instanceof Boolean) {
					result.pushBoolean((Boolean) value);
				} else if (value instanceof Integer) {
					result.pushInt((Integer) value);
				} else if (value instanceof Long) {
					result.pushString(String.valueOf(value));
				} else if (value instanceof String) {
					result.pushString((String) value);
				} else if (value instanceof Double) {
					result.pushDouble((Double) value);
				} else {
					PWLog.error(PushwooshPlugin.TAG, "Could not convert object " + value.toString());
				}
			} catch (JSONException e) {
				e.printStackTrace();
				PWLog.error(PushwooshPlugin.TAG, "Could not convert object with index i " + i + " in array " + array.toString(), e);
			}
		}

		return result;
	}
}