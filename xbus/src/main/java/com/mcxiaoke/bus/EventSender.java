package com.mcxiaoke.bus;

import android.util.Log;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:42
 */
public class EventSender {
    private static final String TAG=EventSender.class.getSimpleName();

    public final Object event;
    public final Subscriber subscriber;
    public final Class<?> eventType;

    public EventSender(final Object event, final Subscriber subscriber) {
        this.event = event;
        this.subscriber = subscriber;
        this.eventType = event.getClass();
    }

    public Object send() {
        try {
            Log.v(TAG, "send event:[" + eventType.getSimpleName()
                    + "] to subscriber:[" + subscriber.targetType.getSimpleName() + "]");
            return subscriber.invoke(event);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
}
