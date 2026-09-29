package com.pushwoosh.reactnativeplugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableType;
import com.pushwoosh.inbox.ui.PushwooshInboxStyle;
import com.pushwoosh.inbox.ui.model.customizing.formatter.InboxDateFormatter;
import com.pushwoosh.internal.utils.PWLog;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class InboxUiStyleManager {
    private static final String LIST_ERROR_MESSAGE_KEY = "listErrorMessage";
    private static final String LIST_EMPTY_MESSAGE_KEY = "listEmptyMessage";
    private static final String DATE_FORMAT_KEY = "dateFormat";
    private static final String DEFAULT_IMAGE_ICON_KEY = "defaultImageIcon";
    private static final String LIST_ERROR_IMAGE_KEY = "listErrorImage";
    private static final String LIST_EMPTY_IMAGE_KEY = "listEmptyImage";
    private static final String ACCENT_COLOR_KEY = "accentColor";
    private static final String HIGHLIGHT_COLOR_KEY = "highlightColor";
    private static final String BACKGROUND_COLOR_KEY = "backgroundColor";
    private static final String DIVIDER_COLOR_KEY = "dividerColor";
    private static final String DATE_COLOR_KEY = "dateColor";
    private static final String READ_DATE_COLOR_KEY = "readDateColor";
    private static final String TITLE_COLOR_KEY = "titleColor";
    private static final String READ_TITLE_COLOR_KEY = "readTitleColor";
    private static final String DESCRIPTION_COLOR_KEY = "descriptionColor";
    private static final String READ_DESCRIPTION_COLOR_KEY = "readDescriptionColor";
    private static final String BAR_BACKGROUND_COLOR = "barBackgroundColor";
    private static final String BAR_ACCENT_COLOR = "barAccentColor";
    private static final String BAR_TEXT_COLOR = "barTextColor";

    public static final String URI_KEY = "uri";

    private Context context;

    public InboxUiStyleManager(Context context){
        this.context = context;
    }

    public void setStyle(ReadableMap mapStyle) {
        setDateFormat(mapStyle);
        setImages(mapStyle);
        setTexts(mapStyle);
        setColors(mapStyle);
    }

    private void setDateFormat(ReadableMap mapStyle) {
        String dateFormat = getString(mapStyle, DATE_FORMAT_KEY);
        if (dateFormat != null && !dateFormat.isEmpty()) {
            try {
                PushwooshInboxStyle.INSTANCE.setDateFormatter(new ReactInboxDateFormatter(dateFormat));
            } catch (IllegalArgumentException e) {
                PWLog.error(PushwooshPlugin.TAG, "Invalid inbox dateFormat pattern: " + dateFormat, e);
            }
        }
    }

    private void setTexts(ReadableMap mapStyle) {
        PushwooshInboxStyle PWInboxStyle = PushwooshInboxStyle.INSTANCE;
        String listErrorMessage = getString(mapStyle, LIST_ERROR_MESSAGE_KEY);
        if (listErrorMessage != null)
            PWInboxStyle.setListErrorMessage(listErrorMessage);
        String listEmptyMessage = getString(mapStyle, LIST_EMPTY_MESSAGE_KEY);
        if (listEmptyMessage != null)
            PWInboxStyle.setListEmptyText(listEmptyMessage);
    }

    // A production ReadableNativeMap throws out of presentInboxUI() on a typed read of a
    // mismatched or null value, so the type is checked before the value is taken.
    private boolean hasTypedValue(ReadableMap mapStyle, String key, ReadableType type, String label) {
        if (!mapStyle.hasKey(key)) {
            return false;
        }
        if (mapStyle.getType(key) != type) {
            PWLog.error(PushwooshPlugin.TAG, "Inbox style " + key + " must be " + label + "; ignored.");
            return false;
        }
        return true;
    }

    private String getString(ReadableMap mapStyle, String key) {
        return hasTypedValue(mapStyle, key, ReadableType.String, "a string") ? mapStyle.getString(key) : null;
    }

    private Integer getColor(ReadableMap mapStyle, String key) {
        return hasTypedValue(mapStyle, key, ReadableType.Number, "a number") ? mapStyle.getInt(key) : null;
    }

    private void setImages(ReadableMap mapStyle) {
        PushwooshInboxStyle PWInboxStyle = PushwooshInboxStyle.INSTANCE;
        Drawable defaultImageIcon = getImage(mapStyle, DEFAULT_IMAGE_ICON_KEY);
        if (defaultImageIcon != null)
            PWInboxStyle.setDefaultImageIconDrawable(defaultImageIcon);

        Drawable listErrorImage = getImage(mapStyle, LIST_ERROR_IMAGE_KEY);
        if (listErrorImage != null)
            PWInboxStyle.setListErrorImageDrawable(listErrorImage);

        Drawable listEmptyImage = getImage(mapStyle, LIST_EMPTY_IMAGE_KEY);
        if (listEmptyImage != null)
            PWInboxStyle.setListEmptyImageDrawable(listEmptyImage);
    }

    private Drawable getImage(ReadableMap mapStyle, String key) {
        if (!hasTypedValue(mapStyle, key, ReadableType.Map, "an object with a uri")) {
            return null;
        }
        ReadableMap imageMap = mapStyle.getMap(key);
        if (!imageMap.hasKey(URI_KEY) || imageMap.getType(URI_KEY) != ReadableType.String) {
            PWLog.error(PushwooshPlugin.TAG, "Inbox style " + key + "." + URI_KEY + " must be a string; ignored.");
            return null;
        }
        String uri = imageMap.getString(URI_KEY);
        try {
            return getDrawable(uri);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private Drawable getDrawable(String uri) throws IOException {
        URL url = new URL(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
        Drawable drawable = null;
        if (context != null) {
            drawable = new BitmapDrawable(context.getResources(), bitmap);
        }
        return drawable;
    }

    private void setColors(ReadableMap mapStyle) {
        PushwooshInboxStyle PWInboxStyle = PushwooshInboxStyle.INSTANCE;

        Integer accentColor = getColor(mapStyle, ACCENT_COLOR_KEY);
        if (accentColor != null)
            PWInboxStyle.setAccentColor(accentColor);
        Integer highlightColor = getColor(mapStyle, HIGHLIGHT_COLOR_KEY);
        if (highlightColor != null)
            PWInboxStyle.setHighlightColor(highlightColor);
        Integer backgroundColor = getColor(mapStyle, BACKGROUND_COLOR_KEY);
        if (backgroundColor != null)
            PWInboxStyle.setBackgroundColor(backgroundColor);
        Integer dividerColor = getColor(mapStyle, DIVIDER_COLOR_KEY);
        if (dividerColor != null)
            PWInboxStyle.setDividerColor(dividerColor);

        Integer dateColor = getColor(mapStyle, DATE_COLOR_KEY);
        if (dateColor != null)
            PWInboxStyle.setDateColor(dateColor);
        Integer readDateColor = getColor(mapStyle, READ_DATE_COLOR_KEY);
        if (readDateColor != null)
            PWInboxStyle.setReadDateColor(readDateColor);

        Integer titleColor = getColor(mapStyle, TITLE_COLOR_KEY);
        if (titleColor != null)
            PWInboxStyle.setTitleColor(titleColor);
        Integer readTitleColor = getColor(mapStyle, READ_TITLE_COLOR_KEY);
        if (readTitleColor != null)
            PWInboxStyle.setReadTitleColor(readTitleColor);

        Integer descriptionColor = getColor(mapStyle, DESCRIPTION_COLOR_KEY);
        if (descriptionColor != null)
            PWInboxStyle.setDescriptionColor(descriptionColor);
        Integer readDescriptionColor = getColor(mapStyle, READ_DESCRIPTION_COLOR_KEY);
        if (readDescriptionColor != null)
            PWInboxStyle.setReadDescriptionColor(readDescriptionColor);

        Integer barBackgroundColor = getColor(mapStyle, BAR_BACKGROUND_COLOR);
        if (barBackgroundColor != null)
            PWInboxStyle.setBarBackgroundColor(barBackgroundColor);
        Integer barAccentColor = getColor(mapStyle, BAR_ACCENT_COLOR);
        if (barAccentColor != null)
            PWInboxStyle.setBarAccentColor(barAccentColor);
        Integer barTextColor = getColor(mapStyle, BAR_TEXT_COLOR);
        if (barTextColor != null)
            PWInboxStyle.setBarTextColor(barTextColor);
    }

    private class ReactInboxDateFormatter implements InboxDateFormatter {

        private SimpleDateFormat simpleDateFormat;

        public ReactInboxDateFormatter(String dateFormat) {
            Locale aDefault = Locale.getDefault();
            simpleDateFormat = new SimpleDateFormat(dateFormat, aDefault);
        }

        @Override
        public String transform(Date date) {
            return simpleDateFormat.format(date);
        }
    }
}
