package com.pushwoosh.reactnativeplugin;

import com.facebook.react.uimanager.SimpleViewManager;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.uimanager.annotations.ReactProp;
import com.pushwoosh.inapp.view.inline.InlineInAppView;

public class InlineInAppManager extends SimpleViewManager<InlineInAppView> {
    public static final String REACT_CLASS = "PWInlineInAppView";

    @Override
    public String getName() {
        return REACT_CLASS;
    }

    @Override
    public InlineInAppView createViewInstance(ThemedReactContext context) {
        return new InlineInAppView(context);
    }

    @ReactProp(name = "identifier")
    public void setIdentifier(InlineInAppView view, String identifier) {
        view.setIdentifier(identifier);
    }
}
