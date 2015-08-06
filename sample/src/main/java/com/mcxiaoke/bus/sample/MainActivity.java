package com.mcxiaoke.bus.sample;

import android.os.Bundle;
import android.os.SystemClock;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;
import butterknife.Bind;
import butterknife.ButterKnife;
import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.Bus.EventMode;
import com.mcxiaoke.bus.annotation.BusReceiver;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
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

    private static final DateFormat FORMAT = new SimpleDateFormat("HH:mm:ss.SSS", Locale.US);
    @Bind(android.R.id.text1)
    TextView mTextView;

    private void appendLog(final CharSequence text) {
        mTextView.post(new Runnable() {
            @Override
            public void run() {
                mTextView.append(FORMAT.format(new Date()) + " - " + text + "\n");
            }
        });
    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.act_main);
        ButterKnife.bind(this);
        Bus.getDefault().setDebug(true);
        Bus.getDefault().register(this);
        final ExecutorService executor = Executors.newCachedThreadPool();
        for (int j = 0; j < 5; j++) {
            final Runnable runnable = new Runnable() {
                //
                @BusReceiver(mode = EventMode.Sender)
                public void innerEvent(final String event) {
                    Log.v(TAG, "innerEvent=" + Thread.currentThread().getName());
                }

                @Override
                public void run() {
                    long start = System.nanoTime();
                    appendLog("post() start");
                    SystemClock.sleep(1000);
                    Bus.getDefault().post(new StringBuilder("StringBuilder"));
                    Bus.getDefault().register(this);
                    Bus.getDefault().post(new RuntimeException("RuntimeException"));
                    Bus.getDefault().unregister(this);
                    Bus.getDefault().post("HelloWorldString");
                    long end = System.nanoTime();
                    Log.v(TAG, "post() elapsed time=" + (end - start) / 1000000);
                }
            };
            executor.submit(runnable);
            Bus.getDefault().post("SomeBody");
        }

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bus.getDefault().unregister(this);
    }

    @BusReceiver(mode = EventMode.Sender)
    public void senderEvent(final CharSequence event) {
        Log.v(TAG, "senderEvent=" + Thread.currentThread().getName());
        appendLog("senderEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver(mode = EventMode.Thread)
    public void threadEvent(final CharSequence event) {
        Log.v(TAG, "threadEvent=" + Thread.currentThread().getName());
        appendLog("threadEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver(mode = EventMode.Thread)
    public void onThreadEvent(final String event) {
        Log.v(TAG, "onThreadEvent=" + Thread.currentThread().getName());
        appendLog("onThreadEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver(mode = EventMode.Sender)
    public void onSenderEvent(final String event) {
        Log.v(TAG, "onSenderEvent=" + Thread.currentThread().getName());
        appendLog("onSenderEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    @BusReceiver
    public void mainEvent(final String event) {
        Log.v(TAG, "mainEvent=" + Thread.currentThread().getName());
        appendLog("mainEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver(mode = EventMode.Sender)
    protected void protectedMethod(final StringBuilder event) {
        appendLog("senderEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    void defaultMethod(final String event) {
        appendLog("defaultEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    private void privateMethod(final CharSequence event) {
        appendLog("onEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    public static void staticMethod(final CharSequence event) {
        Log.v(TAG, "onEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    public void twoParametersMethod(final CharSequence event, final Object object) {
        appendLog("onEvent event=" + event
                + " thread=" + Thread.currentThread().getName());
    }

    //@BusReceiver
    public void noParameterMethod() {
        appendLog("onEvent event=" + null
                + " thread=" + Thread.currentThread().getName());
    }
}
