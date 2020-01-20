package com.pushwoosh.reactnativeplugin;

import com.facebook.react.common.MapBuilder;
import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

import java.util.Map;

public class InlineInAppManager extends SimpleViewManager<RCTInlineInAppView> {
    public static final String REACT_CLASS = "PWInlineInAppView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RCTInlineInAppView createViewInstance(ThemedReactContext context) {
        RCTInlineInAppView view = new RCTInlineInAppView(context);
        return view;
    }

    @ReactProp(name = "identifier")
    public void setIdentifier(final RCTInlineInAppView view, String identifier) {
        view.setIdentifier(identifier);
    }

    public Map getExportedCustomBubblingEventTypeConstants() {
        return MapBuilder.builder()
                .put(
                        "onLoaded",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onLoaded")))
                .put(
                        "onClosed",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onClosed")))
                .put(
                        "onSizeChanged",
                        MapBuilder.of(
                                "phasedRegistrationNames",
                                MapBuilder.of("bubbled", "onSizeChanged")))
                .build();
    }
}
