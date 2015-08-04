package com.mcxiaoke.bus.sample;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.BusMode;
import com.mcxiaoke.bus.BusReceiver;

import java.util.Random;

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
        Bus.getDefault().register(this);
        setContentView(R.layout.act_main);
        final Random random = new Random();
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                for (int i = 0; i < 10; i++) {
                    SystemClock.sleep(random.nextInt(1000));
                    Log.v(TAG, "start send event: Event " + i);
                    Bus.getDefault().post("Event " + i);
                }

            }
        };
        new Thread(runnable).start();
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
