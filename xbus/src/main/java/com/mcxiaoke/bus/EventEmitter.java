package com.mcxiaoke.bus;

import android.util.Log;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:42
 */
public class EventEmitter implements Runnable {
    private static final String TAG = EventEmitter.class.getSimpleName();

    public final Object event;
    public final Subscriber subscriber;

    public EventEmitter(final Object event, final Subscriber subscriber) {
        this.event = event;
        this.subscriber = subscriber;
    }

    @Override
    public void run() {
        try {
            Log.v(TAG, "send event:[" + event.getClass().getSimpleName()
                    + "] to subscriber:[" + subscriber + "]");
            subscriber.invoke(event);
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
