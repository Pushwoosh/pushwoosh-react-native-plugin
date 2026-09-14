package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.module.model.ReactModuleInfo;
import com.facebook.react.turbomodule.core.interfaces.TurboModule;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * Compiled only when the Gradle switch picked src/newarch, i.e. the codegen TurboModule. The
 * runner (tools/android-test.bash) checks that this suite ran on the New Architecture matrix rows.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class NewArchitectureTest {

    @Rule
    public final PluginTestRule pluginRule = new PluginTestRule();

    // Verifies that the module is the codegen TurboModule and is registered as one, so
    // TurboModuleManager rather than the legacy bridge owns it.
    @Test
    public void testPluginIsRegisteredAsTurboModule() {
        ReactModuleInfo info = new PushwooshPackage().getReactModuleInfoProvider().getReactModuleInfos().get(PushwooshPlugin.MODULE_NAME);

        assertTrue(TurboModule.class.isAssignableFrom(PushwooshPlugin.class));
        assertTrue(NativePushwooshSpec.class.isAssignableFrom(PushwooshPlugin.class));
        assertTrue(info.isTurboModule());
    }

    // Verifies that the module answers to the name codegen wrote into the spec: it is the name JS
    // passes to TurboModuleRegistry.get(), and the only one the C++ side will look up.
    @Test
    public void testModuleNameMatchesGeneratedSpec() {
        PushwooshPlugin plugin = new PushwooshPlugin(mock(ReactApplicationContext.class));

        assertEquals(NativePushwooshSpec.NAME, PushwooshPlugin.MODULE_NAME);
        assertEquals(NativePushwooshSpec.NAME, plugin.getName());
    }
}
