package com.pushwoosh.reactnativeplugin;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;

public class InlineInAppManager extends SimpleViewManager<RCTInlineInAppView> {
    public static final String REACT_CLASS = "PWInlineInAppView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public RCTInlineInAppView createViewInstance(ThemedReactContext context) {
        return new RCTInlineInAppView(context);
    }


    @ReactProp(name = "identifier")
    public void setIdentifier(final RCTInlineInAppView view, String identifier) {
        view.setIdentifier(identifier);
    }
}
