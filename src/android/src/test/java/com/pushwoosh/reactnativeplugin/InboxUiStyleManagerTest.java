package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.react.bridge.JavaOnlyMap;
import com.facebook.react.bridge.NoSuchKeyException;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.pushwoosh.inbox.ui.PushwooshInboxStyle;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
public class InboxUiStyleManagerTest {

    private Locale defaultLocale;

    // The manager formats dates with the default locale, and a locale with its own digits would
    // render 10.09.2026 in them.
    @Before
    public void setUp() {
        defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.US);
    }

    @After
    public void tearDown() {
        Locale.setDefault(defaultLocale);
    }

    // Verifies that the style keys the JS API documents land on the matching PushwooshInboxStyle
    // properties; a renamed key would silently leave the inbox unstyled.
    @Test
    public void testSetStyleAppliesJsKeysToInboxStyle() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());

        manager.setStyle(JavaOnlyMap.of(
                "accentColor", 0xFF112233,
                "barTextColor", 0xFF445566,
                "listEmptyMessage", "Nothing here yet",
                "listErrorMessage", "Could not load",
                "dateFormat", "dd.MM.yyyy"));

        PushwooshInboxStyle style = PushwooshInboxStyle.INSTANCE;
        assertEquals(Integer.valueOf(0xFF112233), style.getAccentColor());
        assertEquals(Integer.valueOf(0xFF445566), style.getBarTextColor());
        assertEquals("Nothing here yet", style.getListEmptyText().toString());
        assertEquals("Could not load", style.getListErrorMessage().toString());
        assertEquals("10.09.2026", style.getDateFormatter().transform(new GregorianCalendar(2026, Calendar.SEPTEMBER, 10).getTime()));
    }

    // Verifies that an image key holding null is skipped and the keys after it still land:
    // getMap() answers null for it and the uri lookup used to throw out of presentInboxUI().
    @Test
    public void testSetStyleSkipsImageWhenValueIsNull() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());

        manager.setStyle(JavaOnlyMap.of(
                "defaultImageIcon", null,
                "listEmptyMessage", "No icon, no crash"));

        assertEquals("No icon, no crash", PushwooshInboxStyle.INSTANCE.getListEmptyText().toString());
    }

    // Verifies that an image key holding a string instead of an {uri} object is skipped and the
    // keys after it still land: getMap() used to fail the cast and take presentInboxUI() down.
    @Test
    public void testSetStyleSkipsImageWhenValueIsNotAMap() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());

        manager.setStyle(JavaOnlyMap.of(
                "listErrorImage", "https://img.example/error.png",
                "listErrorMessage", "No object, no crash"));

        assertEquals("No object, no crash", PushwooshInboxStyle.INSTANCE.getListErrorMessage().toString());
    }

    // Verifies that an image object whose uri is a number is skipped and the keys after it still
    // land: getString() on the uri used to throw out of presentInboxUI().
    @Test
    public void testSetStyleSkipsImageWhenUriIsNotAString() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());

        manager.setStyle(JavaOnlyMap.of(
                "defaultImageIcon", JavaOnlyMap.of("uri", 42),
                "listEmptyMessage", "Numeric uri, no crash"));

        assertEquals("Numeric uri, no crash", PushwooshInboxStyle.INSTANCE.getListEmptyText().toString());
    }

    // Verifies that an image object without a uri key is skipped: a production ReadableNativeMap
    // throws NoSuchKeyException from getType() on a missing key, so hasKey() must come first.
    @Test
    public void testSetStyleSkipsImageWhenUriKeyIsMissing() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());
        ReadableMap imageWithoutUri = mock(ReadableMap.class);
        when(imageWithoutUri.hasKey("uri")).thenReturn(false);
        when(imageWithoutUri.getType("uri")).thenThrow(new NoSuchKeyException("uri"));
        when(imageWithoutUri.getString("uri")).thenReturn(null);

        manager.setStyle(JavaOnlyMap.of(
                "listEmptyImage", imageWithoutUri,
                "listEmptyMessage", "Missing uri, no crash"));

        assertEquals("Missing uri, no crash", PushwooshInboxStyle.INSTANCE.getListEmptyText().toString());
    }

    // Verifies that an image object whose uri is null is skipped without ever calling getString():
    // the value has ReadableType.Null and must be rejected by the type check alone.
    @Test
    public void testSetStyleSkipsImageWhenUriIsNull() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());
        ReadableMap imageWithNullUri = mock(ReadableMap.class);
        when(imageWithNullUri.hasKey("uri")).thenReturn(true);
        when(imageWithNullUri.getType("uri")).thenReturn(ReadableType.Null);
        when(imageWithNullUri.getString("uri")).thenReturn(null);

        manager.setStyle(JavaOnlyMap.of(
                "listErrorImage", imageWithNullUri,
                "listErrorMessage", "Null uri, no crash"));

        assertEquals("Null uri, no crash", PushwooshInboxStyle.INSTANCE.getListErrorMessage().toString());
        verify(imageWithNullUri, never()).getString("uri");
    }

    // Verifies that an invalid date pattern keeps the formatter already in place and still applies
    // the keys after it: SimpleDateFormat rejects the pattern with IllegalArgumentException, which
    // used to escape presentInboxUI().
    @Test
    public void testSetStyleKeepsPreviousFormatterWhenDateFormatIsInvalid() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());
        manager.setStyle(JavaOnlyMap.of("dateFormat", "dd.MM.yyyy"));

        manager.setStyle(JavaOnlyMap.of(
                "dateFormat", "dd 'MMMM",
                "listEmptyMessage", "Still styled"));

        PushwooshInboxStyle style = PushwooshInboxStyle.INSTANCE;
        assertEquals("10.09.2026", style.getDateFormatter().transform(new GregorianCalendar(2026, Calendar.SEPTEMBER, 10).getTime()));
        assertEquals("Still styled", style.getListEmptyText().toString());
    }

    // Verifies that a text key holding a number is skipped and the keys after it still land:
    // getString() used to throw out of presentInboxUI().
    @Test
    public void testSetStyleSkipsTextWhenValueIsNotAString() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());

        manager.setStyle(JavaOnlyMap.of(
                "listEmptyMessage", 42,
                "listErrorMessage", "Numeric empty text, no crash"));

        assertEquals("Numeric empty text, no crash", PushwooshInboxStyle.INSTANCE.getListErrorMessage().toString());
    }

    // Verifies that a text key holding null is ignored instead of wiping the text already applied,
    // and that the keys after it still land.
    @Test
    public void testSetStyleSkipsTextWhenValueIsNull() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());
        manager.setStyle(JavaOnlyMap.of("listErrorMessage", "Kept error text"));

        manager.setStyle(JavaOnlyMap.of(
                "listErrorMessage", null,
                "listEmptyMessage", "Null error text, no crash"));

        PushwooshInboxStyle style = PushwooshInboxStyle.INSTANCE;
        assertEquals("Kept error text", style.getListErrorMessage().toString());
        assertEquals("Null error text, no crash", style.getListEmptyText().toString());
    }

    // Verifies that a dateFormat holding a number keeps the formatter already in place and still
    // applies the keys after it: getString() used to throw out of presentInboxUI().
    @Test
    public void testSetStyleSkipsDateFormatWhenValueIsNotAString() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());
        manager.setStyle(JavaOnlyMap.of("dateFormat", "dd.MM.yyyy"));

        manager.setStyle(JavaOnlyMap.of(
                "dateFormat", 42,
                "listEmptyMessage", "Numeric dateFormat, no crash"));

        PushwooshInboxStyle style = PushwooshInboxStyle.INSTANCE;
        assertEquals("10.09.2026", style.getDateFormatter().transform(new GregorianCalendar(2026, Calendar.SEPTEMBER, 10).getTime()));
        assertEquals("Numeric dateFormat, no crash", style.getListEmptyText().toString());
    }

    // Verifies that a dateFormat holding null keeps the formatter already in place and still
    // applies the keys after it.
    @Test
    public void testSetStyleSkipsDateFormatWhenValueIsNull() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());
        manager.setStyle(JavaOnlyMap.of("dateFormat", "dd.MM.yyyy"));

        manager.setStyle(JavaOnlyMap.of(
                "dateFormat", null,
                "listEmptyMessage", "Null dateFormat, no crash"));

        PushwooshInboxStyle style = PushwooshInboxStyle.INSTANCE;
        assertEquals("10.09.2026", style.getDateFormatter().transform(new GregorianCalendar(2026, Calendar.SEPTEMBER, 10).getTime()));
        assertEquals("Null dateFormat, no crash", style.getListEmptyText().toString());
    }

    // Verifies that a colour key holding a string is skipped and the colours after it still land:
    // getInt() used to throw out of presentInboxUI().
    @Test
    public void testSetStyleSkipsColorWhenValueIsNotANumber() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());

        manager.setStyle(JavaOnlyMap.of(
                "accentColor", "#ff0000",
                "titleColor", 0xFF010203));

        assertEquals(Integer.valueOf(0xFF010203), PushwooshInboxStyle.INSTANCE.getTitleColor());
    }

    // Verifies that a colour key holding null is skipped and the colours after it still land:
    // getInt() used to throw out of presentInboxUI().
    @Test
    public void testSetStyleSkipsColorWhenValueIsNull() {
        InboxUiStyleManager manager = new InboxUiStyleManager(RuntimeEnvironment.getApplication());

        manager.setStyle(JavaOnlyMap.of(
                "accentColor", null,
                "barTextColor", 0xFF040506));

        assertEquals(Integer.valueOf(0xFF040506), PushwooshInboxStyle.INSTANCE.getBarTextColor());
    }
}
