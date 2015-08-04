package com.mcxiaoke.bus;

import android.util.Log;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:42
 */
public class EventSender implements Runnable {
    private static final String TAG = EventSender.class.getSimpleName();

    public final Object event;
    public final Subscriber subscriber;

    public EventSender(final Object event, final Subscriber subscriber) {
        this.event = event;
        this.subscriber = subscriber;
    }

    @Override
    public void run() {
        send();
    }

    public Object send() {
        try {
            Log.v(TAG, "send event:[" + event.getClass().getSimpleName()
                    + "] to subscriber:[" + subscriber + "]");
            return subscriber.invoke(event);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toString() {
        return "EventSender{" +
                "event=" + event +
                ", subscriber=" + subscriber +
                '}';
    }
}
