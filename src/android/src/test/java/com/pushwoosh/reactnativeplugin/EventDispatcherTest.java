package com.pushwoosh.reactnativeplugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.facebook.react.bridge.Callback;

import org.junit.Test;

public class EventDispatcherTest {

    // Verifies that a subscriber is invoked once and then dropped: React Native lets a Callback be
    // invoked a single time and throws on the second call.
    @Test
    public void testDispatchEventInvokesEachSubscriberOnlyOnce() {
        EventDispatcher dispatcher = new EventDispatcher();
        Callback first = mock(Callback.class);
        Callback second = mock(Callback.class);
        dispatcher.subscribe("push", first);
        dispatcher.subscribe("push", second);

        dispatcher.dispatchEvent("push", "payload");
        dispatcher.dispatchEvent("push", "payload");

        verify(first, times(1)).invoke("payload");
        verify(second, times(1)).invoke("payload");
    }
}
