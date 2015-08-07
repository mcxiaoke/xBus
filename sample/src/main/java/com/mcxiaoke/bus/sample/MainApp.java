package com.mcxiaoke.bus.sample;

import android.app.Application;
import com.mcxiaoke.bus.Bus;

/**
 * User: mcxiaoke
 * Date: 15/8/7
 * Time: 14:24
 */
public class MainApp extends Application {

    private Bus mBus = new Bus();

    @Override
    public void onCreate() {
        super.onCreate();
    }

    public Bus getBus() {
        return mBus;
    }
}
