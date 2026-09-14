package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.facebook.react.bridge.JavaOnlyArray;
import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.pushwoosh.inbox.data.InboxMessage;
import com.pushwoosh.inbox.data.InboxMessageType;
import com.pushwoosh.tags.TagsBundle;

import java.util.Arrays;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

/**
 * The JS <-> SDK data conversions. JS objects arrive as {@link ReadableMap}s and leave as
 * {@code WritableMap}s; the SDK speaks JSON and {@link TagsBundle}.
 */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class ConversionUtilTest {

    @Rule
    public final PluginTestRule plugin = new PluginTestRule();

    // Verifies that a JS object with a value of every ReadableType reaches the SDK as the matching
    // JSON, nesting included.
    @Test
    public void testToJsonObjectConvertsEveryReadableType() throws JSONException {
        ReadableMap map = JavaOnlyMap.of(
                "text", "hello",
                "number", 2.5,
                "flag", true,
                "nothing", null,
                "nested", JavaOnlyMap.of("key", "value"),
                "list", JavaOnlyArray.of("first", 7));

        JSONObject json = ConversionUtil.toJsonObject(map);

        assertEquals("hello", json.getString("text"));
        assertEquals(2.5, json.getDouble("number"), 0);
        assertTrue(json.getBoolean("flag"));
        assertTrue(json.isNull("nothing"));
        assertEquals("value", json.getJSONObject("nested").getString("key"));
        assertEquals("first", json.getJSONArray("list").getString(0));
        assertEquals(7, json.getJSONArray("list").getInt(1));
    }

    // Verifies that SDK JSON reaches JS with every type mapped, and 64-bit integers as strings:
    // a JS number cannot hold them exactly.
    @Test
    public void testToWritableMapConvertsEveryJsonType() throws JSONException {
        JSONObject json = new JSONObject("{\"int\":1,\"long\":12345678901,\"double\":1.5,"
                + "\"text\":\"hello\",\"flag\":true,\"nothing\":null,"
                + "\"nested\":{\"key\":\"value\"},\"list\":[1,\"two\"]}");

        ReadableMap map = ConversionUtil.toWritableMap(json);

        assertEquals(1, map.getInt("int"));
        assertEquals(ReadableType.String, map.getType("long"));
        assertEquals("12345678901", map.getString("long"));
        assertEquals(1.5, map.getDouble("double"), 0);
        assertEquals("hello", map.getString("text"));
        assertTrue(map.getBoolean("flag"));
        assertTrue(map.isNull("nothing"));
        assertEquals("value", map.getMap("nested").getString("key"));
        assertEquals(1, map.getArray("list").getInt(0));
        assertEquals("two", map.getArray("list").getString(1));
    }

    // Verifies that setTags()/postEvent() attributes keep their values on the way into TagsBundle.
    @Test
    public void testConvertToTagsBundleKeepsValues() throws JSONException {
        ReadableMap map = JavaOnlyMap.of(
                "plan", "pro",
                "visits", 3,
                "interests", JavaOnlyArray.of("news", "sport"));

        TagsBundle bundle = ConversionUtil.convertToTagsBundle(map);

        assertEquals("pro", bundle.getString("plan"));
        assertEquals(3, bundle.getInt("visits", -1));
        JSONArray interests = bundle.toJson().getJSONArray("interests");
        assertEquals("news", interests.getString(0));
        assertEquals("sport", interests.getString(1));
    }

    // Verifies that inbox message codes survive as strings and anything else is dropped instead
    // of failing the whole call.
    @Test
    public void testMessageCodesArrayToArrayListKeepsOnlyStrings() {
        ReadableArray codes = JavaOnlyArray.of("A1", 2, "B3");

        assertEquals(Arrays.asList("A1", "B3"), ConversionUtil.messageCodesArrayToArrayList(codes));
    }

    // Verifies that an inbox message reaches JS with the fields the typings promise and the custom
    // data pulled out of the action params.
    @Test
    public void testInboxMessageToJsonExposesFieldsAndCustomData() throws JSONException {
        InboxMessage message = mock(InboxMessage.class);
        when(message.getCode()).thenReturn("m-1");
        when(message.getTitle()).thenReturn("Title");
        when(message.getMessage()).thenReturn("Body");
        when(message.getImageUrl()).thenReturn("https://img.example/1.png");
        when(message.getBannerUrl()).thenReturn("https://img.example/banner.png");
        when(message.getISO8601SendDate()).thenReturn("2026-09-10T10:00:00Z");
        when(message.getType()).thenReturn(InboxMessageType.URL);
        when(message.isRead()).thenReturn(true);
        when(message.isActionPerformed()).thenReturn(false);
        when(message.getActionParams()).thenReturn("{\"l\":\"https://link.example\",\"u\":\"{\\\"promo\\\":42}\"}");

        JSONObject json = ConversionUtil.inboxMessageToJson(message);

        assertEquals("m-1", json.getString("code"));
        assertEquals("Title", json.getString("title"));
        assertEquals("Body", json.getString("message"));
        assertEquals("https://img.example/1.png", json.getString("imageUrl"));
        assertEquals("https://img.example/banner.png", json.getString("bannerUrl"));
        assertEquals("2026-09-10T10:00:00Z", json.getString("sendDate"));
        assertEquals(InboxMessageType.URL.getCode(), json.getInt("type"));
        assertTrue(json.getBoolean("isRead"));
        assertFalse(json.getBoolean("isActionPerformed"));
        assertEquals("{\"promo\":42}", json.getString("customData"));
    }
}
