package com.pushwoosh.reactnativeplugin;

import static org.junit.Assert.assertEquals;

import com.facebook.react.bridge.JavaOnlyMap;
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
}
