package com.pushwoosh.reactnativeplugin;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.facebook.react.bridge.Callback;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
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

    // Verifies that a subscriber which throws - a callback of a bundle whose runtime is gone, or
    // one React Native has already consumed - does not take the dispatch down with it: the rest of
    // the list is still invoked, the list is still emptied, and the caller still gets to send its
    // device event afterwards. Left unguarded, the bad callback stayed in the list and broke every
    // push that followed.
    @Test
    public void testDispatchEventSurvivesAThrowingSubscriber() {
        EventDispatcher dispatcher = new EventDispatcher();
        Callback throwing = mock(Callback.class);
        Callback next = mock(Callback.class);
        Mockito.doThrow(new RuntimeException("Illegal callback invocation from native module"))
                .when(throwing).invoke("payload");
        dispatcher.subscribe("push", throwing);
        dispatcher.subscribe("push", next);

        dispatcher.dispatchEvent("push", "payload");
        dispatcher.dispatchEvent("push", "payload");

        verify(next, times(1)).invoke("payload");
        verify(throwing, times(1)).invoke("payload");
    }
}
