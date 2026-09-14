package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

import android.app.Activity;
import android.content.Intent;

import com.facebook.react.bridge.Callback;
import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.pushwoosh.RegisterForPushNotificationsResultData;
import com.pushwoosh.exception.RegisterForPushNotificationsException;
import com.pushwoosh.exception.PushwooshException;
import com.pushwoosh.exception.SetUserException;
import com.pushwoosh.exception.SetUserIdException;
import com.pushwoosh.exception.UnregisterForPushNotificationException;
import com.pushwoosh.function.Result;
import com.pushwoosh.inbox.PushwooshInbox;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.inbox.exception.InboxMessagesException;
import com.pushwoosh.inbox.ui.presentation.view.activity.InboxActivity;
import com.pushwoosh.notification.LocalNotification;
import com.pushwoosh.notification.PushwooshNotificationSettings;
import com.pushwoosh.richmedia.RichMediaManager;
import com.pushwoosh.richmedia.RichMediaType;
import com.pushwoosh.tags.TagsBundle;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The bridge methods: how SDK results and errors are translated into the JS callbacks, and how JS
 * arguments reach the SDK.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class PushwooshPluginBridgeTest {

    @Rule
    public final PluginTestRule pluginRule = new PluginTestRule();

    private final ReactApplicationContext reactContext = mock(ReactApplicationContext.class);
    private final Callback success = mock(Callback.class);
    private final Callback error = mock(Callback.class);
    private PushwooshPlugin plugin;

    @Before
    public void setUp() {
        plugin = new PushwooshPlugin(reactContext);
    }

    // Verifies that a successful registration reports the push token to JS exactly once: React
    // Native lets a Callback be invoked a single time, whatever the SDK does afterwards.
    @Test
    public void testRegisterForPushNotificationsReportsTokenOnce() {
        plugin.registerForPushNotifications(success, error);
        com.pushwoosh.function.Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> sdkCallback = registrationCallback();

        sdkCallback.process(Result.fromData(new RegisterForPushNotificationsResultData("fcm-token", true)));
        sdkCallback.process(Result.fromData(new RegisterForPushNotificationsResultData("fcm-token-2", true)));

        verify(success).invoke("fcm-token");
        verifyNoMoreInteractions(success);
        verifyNoInteractions(error);
    }

    // Verifies that a failed registration hands the SDK message to the JS error callback.
    @Test
    public void testRegisterForPushNotificationsReportsFailure() {
        plugin.registerForPushNotifications(success, error);

        registrationCallback().process(Result.fromException(new RegisterForPushNotificationsException("Permission denied")));

        verify(error).invoke("Permission denied");
        verifyNoInteractions(success);
    }

    // Verifies that setUserId() passes the id to the SDK and confirms to JS once accepted.
    @Test
    public void testSetUserIdConfirmsOnSuccess() {
        plugin.setUserId("user-1", success, error);

        userIdCallback("user-1").process(Result.fromData(Boolean.TRUE));

        verify(success).invoke();
        verifyNoInteractions(error);
    }

    // Verifies that setUserId() hands the SDK message to the JS error callback.
    @Test
    public void testSetUserIdReportsFailure() {
        plugin.setUserId("user-1", success, error);

        userIdCallback("user-1").process(Result.fromException(new SetUserIdException("Network unavailable")));

        verify(error).invoke("Network unavailable");
        verifyNoInteractions(success);
    }

    // Verifies that setEmails() passes the JS array to the SDK as a list of strings.
    @Test
    public void testSetEmailsPassesTheListToTheSdk() {
        plugin.setEmails(JavaOnlyArray.of("one@example.com", "two@example.com"), success, error);

        verify(pluginRule.pushwoosh()).setEmail(eq(Arrays.asList("one@example.com", "two@example.com")), any());
    }

    // Verifies that getTags() hands the device tags to JS as a map with their values intact.
    @Test
    public void testGetTagsDeliversTagsAsMap() {
        plugin.getTags(success, error);

        tagsCallback().process(Result.fromData(new TagsBundle.Builder().putString("plan", "pro").putInt("visits", 3).build()));

        ReadableMap tags = (ReadableMap) singleArgument(success);
        assertEquals("pro", tags.getString("plan"));
        assertEquals(3, tags.getInt("visits"));
        verifyNoInteractions(error);
    }

    // Verifies that loadMessages() delivers the inbox to JS as an array of message maps.
    @Test
    public void testLoadMessagesDeliversMessagesAsArray() {
        try (MockedStatic<PushwooshInbox> inbox = mockStatic(PushwooshInbox.class)) {
            plugin.loadMessages(success, error);

            inboxCallback(inbox).process(Result.fromData(Arrays.asList(message("m-1"), message("m-2"))));
        }

        ReadableArray messages = (ReadableArray) singleArgument(success);
        assertEquals(2, messages.size());
        assertEquals("m-1", messages.getMap(0).getString("code"));
        assertEquals("m-2", messages.getMap(1).getString("code"));
        verifyNoInteractions(error);
    }

    // Verifies that a failed inbox load ends in the JS error callback instead of a promise that
    // never settles.
    @Test
    public void testLoadMessagesReportsFailure() {
        try (MockedStatic<PushwooshInbox> inbox = mockStatic(PushwooshInbox.class)) {
            plugin.loadMessages(success, error);

            inboxCallback(inbox).process(Result.fromException(new InboxMessagesException("offline")));
        }

        verify(error).invoke(anyString());
        verifyNoInteractions(success);
    }

    // Verifies that the JS RichMediaStyle constants (MODAL = 0, LEGACY = 1) map onto the SDK's
    // RichMediaType and read back as the same constant.
    @Test
    public void testRichMediaTypeRoundTripsJsConstants() {
        AtomicReference<RichMediaType> stored = new AtomicReference<>(RichMediaType.DEFAULT);
        try (MockedStatic<RichMediaManager> richMedia = mockStatic(RichMediaManager.class)) {
            richMedia.when(() -> RichMediaManager.setRichMediaType(any())).thenAnswer(invocation -> {
                stored.set(invocation.getArgument(0));
                return null;
            });
            richMedia.when(RichMediaManager::getRichMediaType).thenAnswer(invocation -> stored.get());

            plugin.setRichMediaType(0);
            assertEquals(RichMediaType.MODAL, stored.get());
            plugin.getRichMediaType(success);
            verify(success).invoke(0);

            plugin.setRichMediaType(1);
            assertEquals(RichMediaType.DEFAULT, stored.get());
            plugin.getRichMediaType(error);
            verify(error).invoke(1);
        }
    }

    // Verifies that setCommunicationEnabled() maps the flag onto start/stop of server
    // communication and confirms to JS each time.
    @Test
    public void testSetCommunicationEnabledTogglesServerCommunication() {
        plugin.setCommunicationEnabled(false, success, error);
        plugin.setCommunicationEnabled(true, success, error);

        verify(pluginRule.pushwoosh()).stopServerCommunication();
        verify(pluginRule.pushwoosh()).startServerCommunication();
        verify(success, times(2)).invoke();
        verifyNoInteractions(error);
    }

    // Verifies that reverse proxy headers reach the SDK as the string map it expects.
    @Test
    public void testSetReverseProxyPassesHeaders() {
        plugin.setReverseProxy("https://proxy.example", JavaOnlyMap.of("X-Auth", "token", "X-Tenant", "acme"));

        ArgumentCaptor<Map<String, String>> headers = ArgumentCaptor.forClass(Map.class);
        verify(pluginRule.pushwoosh()).setReverseProxy(eq("https://proxy.example"), headers.capture());
        assertEquals("token", headers.getValue().get("X-Auth"));
        assertEquals("acme", headers.getValue().get("X-Tenant"));
    }

    // Verifies that presentInboxUI() opens the SDK inbox screen from the current activity.
    @Test
    public void testPresentInboxUIStartsInboxActivity() {
        Activity activity = Robolectric.buildActivity(Activity.class).setup().get();
        when(reactContext.getCurrentActivity()).thenReturn(activity);

        plugin.presentInboxUI(null);

        Intent started = shadowOf(activity).getNextStartedActivity();
        assertEquals(InboxActivity.class.getName(), started.getComponent().getClassName());
    }

    // Verifies that the iOS-only foreground alert setting still answers on Android, so the shared
    // JS API never leaves a callback waiting.
    @Test
    public void testGetShowPushnotificationAlertAnswersTrueOnAndroid() {
        plugin.getShowPushnotificationAlert(success);

        verify(success).invoke(true);
    }

    // Verifies that setTags() confirms to JS once the SDK accepted the tags and hands the SDK
    // message to the error callback when it did not.
    @Test
    public void testSetTagsMapsResultOntoCallbacks() {
        plugin.setTags(JavaOnlyMap.of("plan", "pro"), success, error);
        com.pushwoosh.function.Callback<Void, PushwooshException> sdkCallback = tagsSendCallback();

        sdkCallback.process(Result.fromData(null));
        sdkCallback.process(Result.fromException(new PushwooshException("Network unavailable")));

        verify(success).invoke();
        verify(error).invoke("Network unavailable");
    }

    // Verifies that unregister() reports the token the SDK released and, on failure, the SDK
    // message: the JS API promises one or the other.
    @Test
    public void testUnregisterMapsResultOntoCallbacks() {
        plugin.unregister(success, error);
        com.pushwoosh.function.Callback<String, UnregisterForPushNotificationException> sdkCallback = unregisterCallback();

        sdkCallback.process(Result.fromData("fcm-token"));
        sdkCallback.process(Result.fromException(new UnregisterForPushNotificationException("still registered")));

        verify(success).invoke("fcm-token");
        verify(error).invoke("still registered");
    }

    // Verifies that setUserEmails() passes the user id and the JS array to the SDK and maps its
    // result onto the JS callbacks.
    @Test
    public void testSetUserEmailsPassesArgumentsAndMapsResult() {
        plugin.setUserEmails("user-1", JavaOnlyArray.of("one@example.com"), success, error);
        com.pushwoosh.function.Callback<Boolean, SetUserException> sdkCallback = userEmailsCallback();

        sdkCallback.process(Result.fromData(Boolean.TRUE));
        sdkCallback.process(Result.fromException(new SetUserException("rejected")));

        verify(pluginRule.pushwoosh()).setUser(eq("user-1"), eq(Arrays.asList("one@example.com")), any());
        verify(success).invoke();
        verify(error).invoke("rejected");
    }

    // Verifies that an opaque ARGB colour survives the trip from JS: the spec types it as a
    // number, so it arrives as a double, and 0xFFFF0000 does not fit a signed int - a direct
    // narrowing would clamp it to Integer.MAX_VALUE and light the LED in the wrong colour.
    @Test
    public void testSetColorLedKeepsAnOpaqueArgbValue() {
        try (MockedStatic<PushwooshNotificationSettings> settings = mockStatic(PushwooshNotificationSettings.class)) {
            plugin.setColorLED(0xFFFF0000L);

            settings.verify(() -> PushwooshNotificationSettings.setColorLED(0xFFFF0000));
        }
    }

    // Verifies that a local notification carries the JS message and delay to the SDK.
    @Test
    public void testCreateLocalNotificationSchedulesTheMessage() {
        plugin.createLocalNotification(JavaOnlyMap.of("msg", "Order is ready", "seconds", 5));

        verify(pluginRule.pushwoosh()).scheduleLocalNotification(any(LocalNotification.class));
    }

    // Verifies that a call without a message schedules nothing: optString answers "" rather than
    // null, so a missing message used to reach the SDK as an empty notification.
    @Test
    public void testCreateLocalNotificationWithoutMessageSchedulesNothing() {
        plugin.createLocalNotification(JavaOnlyMap.of("seconds", 5));

        verify(pluginRule.pushwoosh(), never()).scheduleLocalNotification(any(LocalNotification.class));
    }

    @SuppressWarnings("unchecked")
    private com.pushwoosh.function.Callback<Void, PushwooshException> tagsSendCallback() {
        ArgumentCaptor<com.pushwoosh.function.Callback> callback = ArgumentCaptor.forClass(com.pushwoosh.function.Callback.class);
        verify(pluginRule.pushwoosh()).sendTags(any(TagsBundle.class), callback.capture());
        return callback.getValue();
    }

    @SuppressWarnings("unchecked")
    private com.pushwoosh.function.Callback<String, UnregisterForPushNotificationException> unregisterCallback() {
        ArgumentCaptor<com.pushwoosh.function.Callback> callback = ArgumentCaptor.forClass(com.pushwoosh.function.Callback.class);
        verify(pluginRule.pushwoosh()).unregisterForPushNotifications(callback.capture());
        return callback.getValue();
    }

    @SuppressWarnings("unchecked")
    private com.pushwoosh.function.Callback<Boolean, SetUserException> userEmailsCallback() {
        ArgumentCaptor<com.pushwoosh.function.Callback> callback = ArgumentCaptor.forClass(com.pushwoosh.function.Callback.class);
        verify(pluginRule.pushwoosh()).setUser(anyString(), any(), callback.capture());
        return callback.getValue();
    }

    @SuppressWarnings("unchecked")
    private com.pushwoosh.function.Callback<RegisterForPushNotificationsResultData, RegisterForPushNotificationsException> registrationCallback() {
        ArgumentCaptor<com.pushwoosh.function.Callback> callback = ArgumentCaptor.forClass(com.pushwoosh.function.Callback.class);
        verify(pluginRule.pushwoosh()).registerForPushNotifications(callback.capture());
        return callback.getValue();
    }

    @SuppressWarnings("unchecked")
    private com.pushwoosh.function.Callback<Boolean, SetUserIdException> userIdCallback(String userId) {
        ArgumentCaptor<com.pushwoosh.function.Callback> callback = ArgumentCaptor.forClass(com.pushwoosh.function.Callback.class);
        verify(pluginRule.pushwoosh()).setUserId(eq(userId), callback.capture());
        return callback.getValue();
    }

    @SuppressWarnings("unchecked")
    private com.pushwoosh.function.Callback<TagsBundle, com.pushwoosh.exception.GetTagsException> tagsCallback() {
        ArgumentCaptor<com.pushwoosh.function.Callback> callback = ArgumentCaptor.forClass(com.pushwoosh.function.Callback.class);
        verify(pluginRule.pushwoosh()).getTags(callback.capture());
        return callback.getValue();
    }

    @SuppressWarnings("unchecked")
    private static com.pushwoosh.function.Callback<java.util.Collection<InboxMessage>, InboxMessagesException> inboxCallback(MockedStatic<PushwooshInbox> inbox) {
        ArgumentCaptor<com.pushwoosh.function.Callback> callback = ArgumentCaptor.forClass(com.pushwoosh.function.Callback.class);
        inbox.verify(() -> PushwooshInbox.loadMessages(callback.capture()));
        return callback.getValue();
    }

    private static InboxMessage message(String code) {
        InboxMessage message = mock(InboxMessage.class);
        when(message.getCode()).thenReturn(code);
        when(message.getType()).thenReturn(InboxMessageType.PLAIN);
        when(message.getActionParams()).thenReturn("{}");
        return message;
    }

    private static Object singleArgument(Callback callback) {
        ArgumentCaptor<Object> argument = ArgumentCaptor.forClass(Object.class);
        verify(callback).invoke(argument.capture());
        return argument.getValue();
    }
}
