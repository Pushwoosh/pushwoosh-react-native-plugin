package com.pushwoosh.reactnativeplugin;

import android.content.Context;
import android.view.Choreographer;
import android.view.View;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.events.RCTEventEmitter;
import com.pushwoosh.inapp.view.inline.InlineInAppView;
import com.pushwoosh.inapp.view.inline.InlineInAppViewListener;

public class RCTInlineInAppView extends InlineInAppView implements InlineInAppViewListener {
    public RCTInlineInAppView(Context context) {
        super(context);
        this.addInlineInAppViewListener(this);
        setupLayoutHack();
    }

    void setupLayoutHack() {
        Choreographer.getInstance().postFrameCallback(new Choreographer.FrameCallback() {
            @Override
            public void doFrame(long frameTimeNanos) {
                manuallyLayoutChildren();
                getViewTreeObserver().dispatchOnGlobalLayout();
                Choreographer.getInstance().postFrameCallback(this);
            }
        });

    }

    void manuallyLayoutChildren() {
        for (int i = 0; i < getChildCount(); i++) {
            View child = getChildAt(i);
            child.measure(MeasureSpec.makeMeasureSpec(getMeasuredWidth(), MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(getMeasuredHeight(), MeasureSpec.EXACTLY));
            child.layout(0, 0, child.getMeasuredWidth(), child.getMeasuredHeight());
        }
    }

    @Override
    public void onInlineInAppLoaded() {
        WritableMap event = Arguments.createMap();
        event.putString("identifier", this.getIdentifier());
        ReactContext reactContext = (ReactContext)getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onLoaded",
                event);
    }

    @Override
    public void onInlineInAppViewClosed() {
        WritableMap event = Arguments.createMap();
        event.putString("identifier", this.getIdentifier());
        ReactContext reactContext = (ReactContext)getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onClosed",
                event);
    }

    @Override
    public void onInlineInAppViewChangedSize(int var1, int var2) {
        WritableMap event = Arguments.createMap();
        event.putString("width", String.valueOf(var1));
        event.putString("height", String.valueOf(var2));
        ReactContext reactContext = (ReactContext)getContext();
        reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(
                getId(),
                "onSizeChanged",
                event);
    }
}
