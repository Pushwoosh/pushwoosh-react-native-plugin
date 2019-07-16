package com.pushwoosh.reactnativeplugin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import com.facebook.react.bridge.ReadableMap;
import com.pushwoosh.inbox.ui.PushwooshInboxStyle;
import com.pushwoosh.inbox.ui.model.customizing.formatter.InboxDateFormatter;

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
        if (!mapStyle.hasKey(DATE_FORMAT_KEY)) {
            return;
        }
        String dateFormat = mapStyle.getString(DATE_FORMAT_KEY);
        if (dateFormat != null && !dateFormat.isEmpty()) {
            ReactInboxDateFormatter inboxDateFormatter = new ReactInboxDateFormatter(dateFormat);
            PushwooshInboxStyle.INSTANCE.setDateFormatter(inboxDateFormatter);
        }
    }

    private void setTexts(ReadableMap mapStyle) {
        PushwooshInboxStyle PWInboxStyle = PushwooshInboxStyle.INSTANCE;
        if (mapStyle.hasKey(LIST_ERROR_MESSAGE_KEY))
            PWInboxStyle.setListErrorMessage(mapStyle.getString(LIST_ERROR_MESSAGE_KEY));
        if (mapStyle.hasKey(LIST_EMPTY_MESSAGE_KEY))
            PWInboxStyle.setListEmptyText(mapStyle.getString(LIST_EMPTY_MESSAGE_KEY));
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
        if (!mapStyle.hasKey(key)) {
            return null;
        }
        ReadableMap defaultImageIcon = mapStyle.getMap(key);
        String uri = defaultImageIcon.getString(URI_KEY);
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

        if (mapStyle.hasKey(ACCENT_COLOR_KEY))
            PWInboxStyle.setAccentColor(mapStyle.getInt(ACCENT_COLOR_KEY));
        if (mapStyle.hasKey(HIGHLIGHT_COLOR_KEY))
            PWInboxStyle.setHighlightColor(mapStyle.getInt(HIGHLIGHT_COLOR_KEY));
        if (mapStyle.hasKey(BACKGROUND_COLOR_KEY))
            PWInboxStyle.setBackgroundColor(mapStyle.getInt(BACKGROUND_COLOR_KEY));
        if (mapStyle.hasKey(DIVIDER_COLOR_KEY))
            PWInboxStyle.setDividerColor(mapStyle.getInt(DIVIDER_COLOR_KEY));

        if (mapStyle.hasKey(DATE_COLOR_KEY))
            PWInboxStyle.setDateColor(mapStyle.getInt(DATE_COLOR_KEY));
        if (mapStyle.hasKey(READ_DATE_COLOR_KEY))
            PWInboxStyle.setReadDateColor(mapStyle.getInt(READ_DATE_COLOR_KEY));

        if (mapStyle.hasKey(TITLE_COLOR_KEY))
            PWInboxStyle.setTitleColor(mapStyle.getInt(TITLE_COLOR_KEY));
        if (mapStyle.hasKey(READ_TITLE_COLOR_KEY))
            PWInboxStyle.setReadTitleColor(mapStyle.getInt(READ_TITLE_COLOR_KEY));

        if (mapStyle.hasKey(DESCRIPTION_COLOR_KEY))
            PWInboxStyle.setDescriptionColor(mapStyle.getInt(DESCRIPTION_COLOR_KEY));
        if (mapStyle.hasKey(READ_DESCRIPTION_COLOR_KEY))
            PWInboxStyle.setReadDescriptionColor(mapStyle.getInt(READ_DESCRIPTION_COLOR_KEY));

        if (mapStyle.hasKey(BAR_BACKGROUND_COLOR))
            PWInboxStyle.setBarBackgroundColor(mapStyle.getInt(BAR_BACKGROUND_COLOR));
        if (mapStyle.hasKey(BAR_ACCENT_COLOR))
            PWInboxStyle.setBarAccentColor(mapStyle.getInt(BAR_ACCENT_COLOR));
        if (mapStyle.hasKey(BAR_TEXT_COLOR))
            PWInboxStyle.setBarTextColor(mapStyle.getInt(BAR_TEXT_COLOR));
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
