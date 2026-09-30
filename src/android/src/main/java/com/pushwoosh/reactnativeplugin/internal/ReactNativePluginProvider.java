package com.pushwoosh.reactnativeplugin.internal;

import com.pushwoosh.internal.PluginProvider;

public class ReactNativePluginProvider implements PluginProvider {
	private static final String PLUGIN_VERSION = "7.0.3";

	@Override
	public String getPluginType() {
		return "React Native";
	}

	// No @Override: getPluginVersion() lands in PluginProvider only in the next Android SDK release.
	public String getPluginVersion() {
		return PLUGIN_VERSION;
	}

	@Override
	public int richMediaStartDelay() {
		return DEFAULT_RICH_MEDIA_START_DELAY;
	}
}
