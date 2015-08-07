package com.mcxiaoke.bus.sample;

import android.app.Activity;
import android.os.Bundle;
import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.annotation.BusReceiver;

/**
 * User: mcxiaoke
 * Date: 15/8/7
 * Time: 14:11
 */
public class SimpleActivity extends Activity {

    static class SomeEventClass {

    }

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bus.getDefault().register(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Bus.getDefault().unregister(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // you can also register here
        Bus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        // you can also unregister here
        Bus.getDefault().unregister(this);
    }

    @BusReceiver
    public void onStringEvent(String event) {
        // handle your event
    }

    @BusReceiver
    public void onSomeEvent(SomeEventClass event) {
        // handle your event
    }

    @BusReceiver
    public void onObjectEvent(Object event) {
        // handle your event
    }
}
