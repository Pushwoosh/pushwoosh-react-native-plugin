package com.pushwoosh.reactnativeplugin;

import androidx.annotation.Nullable;

import com.facebook.react.BaseReactPackage;
import com.facebook.react.ReactPackage;
import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.module.model.ReactModuleInfoProvider;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;

import java.util.HashMap;
import java.util.Map;

// BaseReactPackage already implements ReactPackage; it is repeated because the autolinking scan
// of the React Native 0.74 CLI only recognises "implements ReactPackage" or "extends TurboReactPackage"
public class PushwooshPackage extends BaseReactPackage implements ReactPackage {

    @Nullable
    @Override
    public NativeModule getModule(String name, ReactApplicationContext reactContext) {
        if (PushwooshPlugin.MODULE_NAME.equals(name)) {
            return new PushwooshPlugin(reactContext);
        }
        return null;
    }

    @Override
    public ReactModuleInfoProvider getReactModuleInfoProvider() {
        return () -> {
            // On the New Architecture PushwooshPluginSpec extends the codegen spec, which is a TurboModule
            boolean isTurboModule = TurboModule.class.isAssignableFrom(PushwooshPlugin.class);
            Map<String, ReactModuleInfo> modules = new HashMap<>();
            modules.put(PushwooshPlugin.MODULE_NAME, new ReactModuleInfo(
                    PushwooshPlugin.MODULE_NAME,
                    PushwooshPlugin.class.getName(),
                    false, // canOverrideExistingModule
                    false, // needsEagerInit: the launch push is cached until JS calls init
                    false, // isCxxModule
                    isTurboModule
            ));
            return modules;
        };
    }
}
