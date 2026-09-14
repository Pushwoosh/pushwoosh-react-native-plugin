package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.facebook.react.bridge.NativeModule;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The package is what React Native asks for the module: TurboModuleManager and the legacy
 * NativeModuleRegistry both go through {@code getModule} and {@code getReactModuleInfoProvider}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PushwooshPackageTest {

    @Rule
    public final PluginTestRule plugin = new PluginTestRule();

    private final PushwooshPackage reactPackage = new PushwooshPackage();
    private final ReactApplicationContext reactContext = mock(ReactApplicationContext.class);

    // Verifies that the module is served under the name JS passes to TurboModuleRegistry.get()
    // and under nothing else: "Pushwoosh" is the SDK class, not the module.
    @Test
    public void testGetModuleServesThePluginUnderItsJsName() {
        NativeModule module = reactPackage.getModule("PushwooshPlugin", reactContext);

        assertTrue(module instanceof PushwooshPlugin);
        assertEquals("PushwooshPlugin", module.getName());
        assertNull(reactPackage.getModule("Pushwoosh", reactContext));
    }

    // Verifies that the module info handed to React Native describes the module the package
    // creates: same name, its class, lazily created, TurboModule exactly when the class is one.
    @Test
    public void testModuleInfoDescribesTheServedModule() {
        Map<String, ReactModuleInfo> infos = reactPackage.getReactModuleInfoProvider().getReactModuleInfos();
        ReactModuleInfo info = infos.get(PushwooshPlugin.MODULE_NAME);

        assertEquals(1, infos.size());
        assertNotNull(info);
        assertEquals(PushwooshPlugin.MODULE_NAME, info.name());
        assertEquals(PushwooshPlugin.class.getName(), info.className());
        assertFalse(info.needsEagerInit());
        assertFalse(info.isCxxModule());
        assertEquals(TurboModule.class.isAssignableFrom(PushwooshPlugin.class), info.isTurboModule());
    }
}
