package com.mcxiaoke.bus.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.Bus.EventMode;
import com.mcxiaoke.bus.annotation.BusReceiver;

import java.io.Serializable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:31
 */
public class MainActivity extends AppCompatActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        Bus.getDefault().setDebug(true).setStrictMode(true);
        Bus.getDefault().register(this);
        final ExecutorService executor = Executors.newCachedThreadPool();
        for (int j = 0; j < 1; j++) {
            final int index = j;
            final Runnable runnable = new Runnable() {

                @BusReceiver(mode = EventMode.Main)
                public void innerEvent(final String event) {
                    Log.v(TAG, "innerEvent event=" + event
                            + " thread=" + Thread.currentThread().getName());
                }

                @Override
                public void run() {
                    long start = System.nanoTime();
                    Log.d(TAG, "post() start");
                    for (int i = 0; i < 5; i++) {
//                        Bus.getDefault().register(this);
                        Bus.getDefault().post(new StringBuilder("Event " + i + " j=" + index));
                        Bus.getDefault().post(new RuntimeException("ErrorEvent"));
                        Bus.getDefault().post(new Runnable() {
                            @Override
                            public void run() {

                            }
                        });
                        Bus.getDefault().post("Hello, Event!");
                        Bus.getDefault().post(new Object());
//                        Bus.getDefault().unregister(this);
                    }
                    long end = System.nanoTime();
                    Log.d(TAG, "post() elapsed time=" + (end - start) / 1000000);
                }
            };
            executor.submit(runnable);
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bus.getDefault().unregister(this);
    }

    @BusReceiver(mode = EventMode.Main)
    public void onEvent(final Serializable event) {
        Log.v(TAG, "mainEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver(mode = EventMode.Thread)
    public void threadEvent(final CharSequence event) {
        Log.v(TAG, "threadEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver(mode = EventMode.Sender)
    public void senderEvent(final StringBuilder event) {
        Log.v(TAG, "senderEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver
    public void onEvent(final String event) {
        Log.v(TAG, "defaultEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    public void onEvent(final Integer event) {
        Log.v(TAG, "onEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    private void privateMethod(final CharSequence event) {
        Log.v(TAG, "onEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    public static void staticMethod(final CharSequence event) {
        Log.v(TAG, "onEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    public void twoParametersMethod(final CharSequence event, final Object object) {
        Log.v(TAG, "onEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    public void noParameterMethod() {
        Log.v(TAG, "onEvent event=" + null
                + " thread=" + Thread.currentThread().getName());
    }
}
