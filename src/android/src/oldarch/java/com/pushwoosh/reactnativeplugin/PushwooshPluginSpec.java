package com.pushwoosh.reactnativeplugin;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;

// Legacy architecture: a plain bridge module whose methods are exported by @ReactMethod.
abstract class PushwooshPluginSpec extends ReactContextBaseJavaModule {
	PushwooshPluginSpec(ReactApplicationContext reactContext) {
		super(reactContext);
	}
}
