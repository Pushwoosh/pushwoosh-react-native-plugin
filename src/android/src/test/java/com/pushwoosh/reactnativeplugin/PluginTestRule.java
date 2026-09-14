package com.pushwoosh.reactnativeplugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.pushwoosh.Pushwoosh;

import java.lang.reflect.Field;

import org.junit.rules.ExternalResource;
import org.mockito.MockedStatic;

/**
 * Puts a test in the state of a freshly started process with a mocked SDK.
 *
 * <p>{@code Arguments.createMap()}/{@code createArray()} return {@code WritableNativeMap}/{@code
 * WritableNativeArray}, which need libreactnativejni; on the JVM the plugin gets the pure-Java
 * {@link JavaOnlyMap}/{@link JavaOnlyArray} instead. {@code Pushwoosh.getInstance()} returns the
 * mock from {@link #pushwoosh()}. Both static mocks are closed after the test.
 *
 * <p>The plugin keeps the launch push, the "JS called init()" flag and the module instance in
 * statics so a push tapped before React Native is up survives until JS is ready. Robolectric
 * shares the class between tests, so they are reset before each one.
 */
final class PluginTestRule extends ExternalResource {

    private MockedStatic<Arguments> arguments;
    private MockedStatic<Pushwoosh> sdk;
    private Pushwoosh pushwoosh;

    Pushwoosh pushwoosh() {
        return pushwoosh;
    }

    @Override
    protected void before() {
        resetPluginStatics();

        arguments = mockStatic(Arguments.class);
        arguments.when(Arguments::createMap).thenAnswer(invocation -> new JavaOnlyMap());
        arguments.when(Arguments::createArray).thenAnswer(invocation -> new JavaOnlyArray());

        pushwoosh = mock(Pushwoosh.class);
        sdk = mockStatic(Pushwoosh.class);
        sdk.when(Pushwoosh::getInstance).thenReturn(pushwoosh);
    }

    @Override
    protected void after() {
        sdk.close();
        arguments.close();
    }

    private static void resetPluginStatics() {
        setStatic("sReceivedPushData", null);
        setStatic("sReceivedPushCallbackRegistered", false);
        setStatic("sStartPushData", null);
        setStatic("sPushCallbackRegistered", false);
        setStatic("sInitialized", false);
        setStatic("INSTANCE", null);
        setStatic("mEventDispatcher", new EventDispatcher());
    }

    private static void setStatic(String name, Object value) {
        try {
            Field field = PushwooshPlugin.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("PushwooshPlugin." + name + " is gone; update PluginTestRule", e);
        }
    }
}
