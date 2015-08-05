package com.mcxiaoke.bus.scheduler;

import android.os.Handler;
import android.os.Looper;
import com.mcxiaoke.bus.Bus;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:42
 */
class HandlerScheduler implements Scheduler {
    private Bus mBus;
    private Handler mHandler;

    public HandlerScheduler(final Bus bus, final Looper looper) {
        mBus = bus;
        mHandler = new Handler(looper);
    }

    @Override
    public void post(final Runnable runnable) {
        mHandler.post(runnable);
    }
}
