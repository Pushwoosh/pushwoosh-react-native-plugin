package com.pushwoosh.reactnativeplugin;

import android.content.Context;
import android.view.Choreographer;
import android.view.View;

import androidx.annotation.NonNull;

import com.pushwoosh.inapp.view.inline.InlineInAppView;

public class RCTInlineInAppView extends InlineInAppView {
    public RCTInlineInAppView(@NonNull Context context) {
        super(context);
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
}
