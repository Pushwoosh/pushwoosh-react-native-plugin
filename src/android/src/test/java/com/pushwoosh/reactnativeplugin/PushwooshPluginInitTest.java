package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * init() and the push that arrives before JS is ready.
 *
 * <p>A push tapped on a cold start reaches {@link PushwooshPlugin#openPush} before the JS bundle
 * has subscribed to anything; the plugin has to hold it until JS calls init() and then deliver it,
 * both as the {@code pushOpened} device event and through the legacy onPushOpen() callback.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PushwooshPluginInitTest {

    // The release gate rejects anything shaped like a real application code (XXXXX-XXXXX).
    private static final String APP_ID = "XXXXX-XXXXX";
    private static final String OPENED_PUSH = "{\"title\":\"Opened\",\"u\":\"{\\\"screen\\\":\\\"inbox\\\"}\"}";
    private static final String RECEIVED_PUSH = "{\"title\":\"Received\"}";
    private static final String LIVE_PUSH = "{\"title\":\"Live\"}";

    @Rule
    public final PluginTestRule pluginRule = new PluginTestRule();

    private final ReactApplicationContext reactContext = mock(ReactApplicationContext.class);
    private final RCTDeviceEventEmitter emitter = mock(RCTDeviceEventEmitter.class);
    private final Callback success = mock(Callback.class);
    private final Callback error = mock(Callback.class);

    @Before
    public void setUp() {
        when(reactContext.getJSModule(RCTDeviceEventEmitter.class)).thenReturn(emitter);
    }

    // Verifies that init() without pw_appid reports the error to JS and leaves the SDK untouched.
    @Test
    public void testInitReportsErrorWhenAppIdMissing() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);

        plugin.init(JavaOnlyMap.of("pw_notification_icon", "ic_notification"), success, error);

        verify(error).invoke("Pushwoosh Application id not specified");
        verifyNoInteractions(success);
        verify(pluginRule.pushwoosh(), never()).setAppId(anyString());
    }

    // Verifies that init() hands the app id to the SDK and confirms to JS.
    @Test
    public void testInitSetsAppIdAndConfirms() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);

        plugin.init(JavaOnlyMap.of("pw_appid", APP_ID), success, error);

        verify(pluginRule.pushwoosh()).setAppId(APP_ID);
        verify(success).invoke();
        verifyNoInteractions(error);
    }

    // Verifies that init() emits no device event when no push preceded it.
    @Test
    public void testInitEmitsNothingWithoutPendingPush() {
        initializedPlugin();

        verifyNoInteractions(emitter);
    }

    // Verifies that a push tapped before JS was ready is emitted as pushOpened as soon as init()
    // runs, so the app can route it.
    @Test
    public void testInitReplaysPushOpenedBeforeJsWasReady() {
        PushwooshPlugin.openPush(OPENED_PUSH);

        initializedPlugin();

        assertEquals("Opened", emitted("pushOpened").getString("title"));
    }

    // Verifies that a push received before JS was ready is emitted as pushReceived on init().
    @Test
    public void testInitReplaysPushReceivedBeforeJsWasReady() {
        PushwooshPlugin.messageReceived(RECEIVED_PUSH);

        initializedPlugin();

        assertEquals("Received", emitted("pushReceived").getString("title"));
    }

    // Verifies that a push arriving while the module exists but JS has not called init() yet is
    // held back rather than emitted into the void.
    @Test
    public void testPushBeforeInitIsHeldBack() {
        new PushwooshPlugin(reactContext);

        PushwooshPlugin.openPush(OPENED_PUSH);
        PushwooshPlugin.messageReceived(RECEIVED_PUSH);

        verifyNoInteractions(emitter);
    }

    // Verifies that once JS has called init(), a push is emitted right away.
    @Test
    public void testPushAfterInitIsEmittedImmediately() {
        initializedPlugin();

        PushwooshPlugin.openPush(LIVE_PUSH);

        assertEquals("Live", emitted("pushOpened").getString("title"));
    }

    // Verifies that onPushOpen() hands the launch push to the first JS callback right away and a
    // later push to whoever subscribed after that.
    @Test
    public void testOnPushOpenDeliversLaunchPushThenLivePush() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);
        PushwooshPlugin.openPush(OPENED_PUSH);
        Callback launch = mock(Callback.class);
        Callback live = mock(Callback.class);

        plugin.onPushOpen(launch);
        plugin.onPushOpen(live);
        PushwooshPlugin.openPush(LIVE_PUSH);

        assertEquals("Opened", ((ReadableMap) singleArgument(launch)).getString("title"));
        assertEquals("Live", ((ReadableMap) singleArgument(live)).getString("title"));
    }

    // Verifies that onPushReceived() hands the push cached before JS was ready to the first
    // callback right away and a later push to whoever subscribed after that. The method is the
    // twin of onPushOpen() over its own pair of statics, where a copy-paste slip would live.
    @Test
    public void testOnPushReceivedDeliversCachedPushThenLivePush() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);
        PushwooshPlugin.messageReceived(RECEIVED_PUSH);
        Callback cached = mock(Callback.class);
        Callback live = mock(Callback.class);

        plugin.onPushReceived(cached);
        plugin.onPushReceived(live);
        PushwooshPlugin.messageReceived(LIVE_PUSH);

        assertEquals("Received", ((ReadableMap) singleArgument(cached)).getString("title"));
        assertEquals("Live", ((ReadableMap) singleArgument(live)).getString("title"));
    }

    // Verifies that the launch push is forgotten with the host: a later init() or onPushOpen()
    // must not resurrect it.
    @Test
    public void testOnHostDestroyForgetsTheLaunchPush() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);
        PushwooshPlugin.openPush(OPENED_PUSH);

        plugin.onHostDestroy();
        plugin.init(JavaOnlyMap.of("pw_appid", APP_ID), success, error);
        plugin.onPushOpen(success);

        verifyNoInteractions(emitter);
        verify(success).invoke();
    }

    // Verifies that a push arriving while the host is down is cached and replayed to the next
    // init(). The flags openPush() checks used to survive the teardown, so the push was handed to
    // a context with no JS runtime, the failure was swallowed, and the cache that would have
    // replayed it had just been cleared by the same teardown.
    @Test
    public void testPushArrivingAfterHostDestroyReachesTheNextInit() {
        PushwooshPlugin plugin = initializedPlugin();
        plugin.onHostDestroy();

        PushwooshPlugin.openPush(LIVE_PUSH);
        initializedPlugin();

        assertEquals("Live", emitted("pushOpened").getString("title"));
    }

    // Verifies that the teardown of a module the host has already replaced leaves the live one
    // alone: the two events are not ordered, so a late onHostDestroy() must not drop the push the
    // new module has cached.
    @Test
    public void testHostDestroyOfAReplacedModuleKeepsTheLiveModulesPush() {
        PushwooshPlugin replaced = new PushwooshPlugin(reactContext);
        PushwooshPlugin live = new PushwooshPlugin(reactContext);
        PushwooshPlugin.openPush(OPENED_PUSH);

        replaced.onHostDestroy();
        live.init(JavaOnlyMap.of("pw_appid", APP_ID), success, error);

        assertEquals("Opened", emitted("pushOpened").getString("title"));
    }

    private PushwooshPlugin initializedPlugin() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);
        plugin.init(JavaOnlyMap.of("pw_appid", APP_ID), success, error);
        return plugin;
    }

    private ReadableMap emitted(String event) {
        ArgumentCaptor<Object> payload = ArgumentCaptor.forClass(Object.class);
        verify(emitter).emit(eq(event), payload.capture());
        return (ReadableMap) payload.getValue();
    }

    private static Object singleArgument(Callback callback) {
        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(callback).invoke(argument.capture());
        return argument.getValue();
    }
}
