package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Compiled only when the Gradle switch picked src/oldarch, i.e. the plain bridge module. The
 * runner (tools/android-test.bash) checks that this suite ran on the legacy matrix row.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class LegacyArchitectureTest {

    // Public methods of the module that are not bridge methods: NativeModule and
    // LifecycleEventListener callbacks.
    private static final Set<String> NOT_BRIDGE_METHODS = new HashSet<>(Arrays.asList(
            "getName", "invalidate", "onHostResume", "onHostPause", "onHostDestroy"));

    @Rule
    public final PluginTestRule pluginRule = new PluginTestRule();

    // Verifies that the module is a plain bridge module and is registered as one, so the legacy
    // NativeModuleRegistry rather than TurboModuleManager owns it.
    @Test
    public void testPluginIsRegisteredAsLegacyModule() {
        ReactModuleInfo info = new PushwooshPackage().getReactModuleInfoProvider().getReactModuleInfos().get(PushwooshPlugin.MODULE_NAME);

        assertFalse(TurboModule.class.isAssignableFrom(PushwooshPlugin.class));
        assertTrue(ReactContextBaseJavaModule.class.isAssignableFrom(PushwooshPlugin.class));
        assertFalse(info.isTurboModule());
    }

    // Verifies that every bridge method carries @ReactMethod: on the legacy bridge the annotation
    // is the only thing that exports a method to JS, and nothing else checks it there.
    @Test
    public void testEveryBridgeMethodIsExportedWithReactMethod() {
        Set<String> notExported = new TreeSet<>();
        for (Method method : PushwooshPlugin.class.getDeclaredMethods()) {
            boolean bridgeCandidate = Modifier.isPublic(method.getModifiers())
                    && !Modifier.isStatic(method.getModifiers())
                    && !NOT_BRIDGE_METHODS.contains(method.getName());
            if (bridgeCandidate && method.getAnnotation(ReactMethod.class) == null) {
                notExported.add(method.getName());
            }
        }

        assertEquals(Collections.emptySet(), notExported);
    }
}
