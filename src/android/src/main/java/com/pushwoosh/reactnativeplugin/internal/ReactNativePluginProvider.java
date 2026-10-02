package com.pushwoosh.reactnativeplugin.internal;

import com.pushwoosh.internal.PluginProvider;

public class ReactNativePluginProvider implements PluginProvider {
	@Override
	public String getPluginType() {
		return "React Native";
	}

	@Override
	public int richMediaStartDelay() {
		return DEFAULT_RICH_MEDIA_START_DELAY;
	}
}
