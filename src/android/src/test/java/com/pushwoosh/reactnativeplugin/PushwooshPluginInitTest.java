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
        when(reactContext.hasActiveReactInstance()).thenReturn(true);
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

    // Verifies that the launch push outlives the activity: a push tapped on a cold start can
    // reach openPush() before JS calls init(), and the user leaving by Back in between must not
    // take it with him - the bundle is still loading and its init() is the only thing that
    // replays it.
    @Test
    public void testHostDestroyKeepsTheLaunchPushForTheBundleStillLoading() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);
        PushwooshPlugin.openPush(OPENED_PUSH);

        plugin.onHostDestroy();
        plugin.init(JavaOnlyMap.of("pw_appid", APP_ID), success, error);

        assertEquals("Opened", emitted("pushOpened").getString("title"));
    }

    // Verifies that a push opened after the activity was destroyed is still emitted while the
    // React instance runs: the user leaves by Back, JS lives on, the SDK starts the activity again
    // and the push must not be parked for an init() JS will never call twice.
    @Test
    public void testPushOpenedAfterHostDestroyIsEmittedWhileJsIsAlive() {
        PushwooshPlugin plugin = initializedPlugin();
        plugin.onHostDestroy();

        PushwooshPlugin.openPush(LIVE_PUSH);

        assertEquals("Live", emitted("pushOpened").getString("title"));
    }

    // Verifies the same for a push received after the activity was destroyed; messageReceived()
    // gates on its own copy of the check.
    @Test
    public void testPushReceivedAfterHostDestroyIsEmittedWhileJsIsAlive() {
        PushwooshPlugin plugin = initializedPlugin();
        plugin.onHostDestroy();

        PushwooshPlugin.messageReceived(LIVE_PUSH);

        assertEquals("Live", emitted("pushReceived").getString("title"));
    }

    // Verifies that once React Native has invalidated the module - the instance is being torn
    // down - a push is held for the next init() rather than handed to a runtime that is going
    // away: a fresh module has to start with "JS has not called init()" again.
    @Test
    public void testPushArrivingAfterInvalidateIsHeldForTheNextInit() {
        PushwooshPlugin plugin = initializedPlugin();
        plugin.onHostDestroy();
        plugin.invalidate();

        PushwooshPlugin.openPush(LIVE_PUSH);
        verifyNoInteractions(emitter);

        initializedPlugin();

        assertEquals("Live", emitted("pushOpened").getString("title"));
    }

    // Verifies that a push arriving while the React instance is already gone but the module has
    // not been invalidated yet is held for the next init() instead of being emitted into a dead
    // runtime, where the broad catch would swallow the failure and the push with it.
    @Test
    public void testPushArrivingWhileTheReactInstanceIsDownIsHeldForTheNextInit() {
        initializedPlugin();
        when(reactContext.hasActiveReactInstance()).thenReturn(false);

        PushwooshPlugin.openPush(LIVE_PUSH);
        verifyNoInteractions(emitter);

        when(reactContext.hasActiveReactInstance()).thenReturn(true);
        initializedPlugin();

        assertEquals("Live", emitted("pushOpened").getString("title"));
    }

    // Verifies that the teardown of a module the runtime has already replaced leaves the live one
    // alone: the two events are not ordered, so an invalidate() arriving after the next bundle has
    // called init() must not close the gate that bundle opened - nothing would open it again, JS
    // calls init() once.
    @Test
    public void testInvalidateOfAReplacedModuleLeavesTheLiveOneAlone() {
        PushwooshPlugin replaced = initializedPlugin();
        PushwooshPlugin live = initializedPlugin();

        replaced.invalidate();
        PushwooshPlugin.openPush(LIVE_PUSH);

        assertEquals("Live", emitted("pushOpened").getString("title"));
        assertEquals(live, liveModule());
    }

    // Verifies that a module React Native constructs during its own teardown does not unseat the
    // live one: TurboModuleManager.invalidate() calls getOrCreateModule() for every holder, so a
    // React instance JS never asked the plugin for still builds one, and that module belongs to no
    // bundle. Claiming the live module's place in the constructor made it take the live bundle's
    // pushes with it when it was invalidated a moment later.
    @Test
    public void testModuleBuiltByTheTeardownDoesNotUnseatTheLiveOne() {
        initializedPlugin();

        PushwooshPlugin stillborn = new PushwooshPlugin(reactContext);
        stillborn.invalidate();
        PushwooshPlugin.openPush(LIVE_PUSH);

        assertEquals("Live", emitted("pushOpened").getString("title"));
    }

    // Verifies that the deprecated onPushOpen() callback also survives the activity: it is a
    // one-shot subscription JS makes once, so dropping its registration with the activity left
    // the callback path dead while the device event path went on working.
    @Test
    public void testLegacyCallbackReceivesPushOpenedAfterHostDestroy() {
        PushwooshPlugin plugin = initializedPlugin();
        Callback subscriber = mock(Callback.class);
        plugin.onPushOpen(subscriber);

        plugin.onHostDestroy();
        PushwooshPlugin.openPush(LIVE_PUSH);

        assertEquals("Live", ((ReadableMap) singleArgument(subscriber)).getString("title"));
    }

    // Verifies that the callbacks of a bundle the teardown took go with it: they are bound to a
    // runtime that is gone, and only the subscriber of the bundle that came after it may be
    // invoked.
    @Test
    public void testInvalidateDropsTheCallbacksOfTheTornDownBundle() {
        PushwooshPlugin plugin = initializedPlugin();
        Callback dead = mock(Callback.class);
        plugin.onPushOpen(dead);
        plugin.invalidate();

        PushwooshPlugin fresh = initializedPlugin();
        Callback live = mock(Callback.class);
        fresh.onPushOpen(live);
        PushwooshPlugin.openPush(LIVE_PUSH);

        verifyNoInteractions(dead);
        assertEquals("Live", ((ReadableMap) singleArgument(live)).getString("title"));
    }

    // Verifies that a push the previous bundle already saw is not replayed to the next one:
    // emitted() accepts a single emit, so a second pushOpened fails it. A replayed pushOpened
    // routes a deep link the app has already handled and sends the user back to the screen the
    // push opened the first time.
    @Test
    public void testAPushDeliveredToOneBundleIsNotReplayedToTheNext() {
        PushwooshPlugin plugin = initializedPlugin();
        PushwooshPlugin.openPush(LIVE_PUSH);
        plugin.invalidate();

        initializedPlugin();

        assertEquals("Live", emitted("pushOpened").getString("title"));
    }

    // Verifies the same for pushReceived; messageReceived() tracks delivery over its own copy of
    // the cache, where a copy-paste slip would live.
    @Test
    public void testAReceivedPushDeliveredToOneBundleIsNotReplayedToTheNext() {
        PushwooshPlugin plugin = initializedPlugin();
        PushwooshPlugin.messageReceived(LIVE_PUSH);
        plugin.invalidate();

        initializedPlugin();

        assertEquals("Live", emitted("pushReceived").getString("title"));
    }

    // Verifies that the next bundle's onPushOpen() subscribes for what is coming instead of being
    // handed the push the bundle before it already handled: the registration is one-shot, so a
    // stale push spent it and left that bundle's callback path dead for good.
    @Test
    public void testTheNextBundlesCallbackSubscribesInsteadOfTakingTheOldPush() {
        PushwooshPlugin plugin = initializedPlugin();
        Callback first = mock(Callback.class);
        plugin.onPushOpen(first);
        PushwooshPlugin.openPush(OPENED_PUSH);
        plugin.invalidate();

        PushwooshPlugin fresh = initializedPlugin();
        Callback next = mock(Callback.class);
        fresh.onPushOpen(next);
        PushwooshPlugin.openPush(LIVE_PUSH);

        assertEquals("Opened", ((ReadableMap) singleArgument(first)).getString("title"));
        assertEquals("Live", ((ReadableMap) singleArgument(next)).getString("title"));
    }

    private PushwooshPlugin initializedPlugin() {
        PushwooshPlugin plugin = new PushwooshPlugin(reactContext);
        plugin.init(JavaOnlyMap.of("pw_appid", APP_ID), success, error);
        return plugin;
    }

    private static PushwooshPlugin liveModule() {
        try {
            java.lang.reflect.Field field = PushwooshPlugin.class.getDeclaredField("INSTANCE");
            field.setAccessible(true);
            return (PushwooshPlugin) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("PushwooshPlugin.INSTANCE is gone", e);
        }
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
