package com.mcxiaoke.bus.sample;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.BusMode;
import com.mcxiaoke.bus.BusReceiver;

import java.util.Random;
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
        Bus.getDefault().register(this);
        final ExecutorService executor = Executors.newCachedThreadPool();
        for (int j = 0; j < 5; j++) {
            final int index = j;
            final Runnable runnable = new Runnable() {

                @BusReceiver(mode = BusMode.Main)
                public void mainEvent1(final Object event) {
                    Log.v(TAG, "mainEvent1 event=" + event
                            + " thread=" + Thread.currentThread().getName());
                }

                @Override
                public void run() {
                    for (int i = 0; i < 5; i++) {
                        Bus.getDefault().register(this);
                        Bus.getDefault().post("Event " + i + " j=" + index);
                        Bus.getDefault().unregister(this);
                    }
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

    @BusReceiver(mode = BusMode.Main)
    public void mainEvent(final Object event) {
        Log.v(TAG, "mainEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver(mode = BusMode.Thread)
    public void threadEvent(final Object event) {
        Log.v(TAG, "threadEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver(mode = BusMode.Sender)
    public void senderEvent(final Object event) {
        Log.v(TAG, "senderEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver
    public void defaultEvent(final Object event) {
        Log.v(TAG, "defaultEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }
}
