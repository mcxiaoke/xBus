package com.mcxiaoke.bus.scheduler;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.EventSender;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:42
 */
class HandlerScheduler implements Scheduler, Handler.Callback {
    private Bus mBus;
    private Handler mHandler;

    public HandlerScheduler(final Bus bus, final Looper looper) {
        mBus = bus;
        mHandler = new Handler(looper, this);
    }


    @Override
    public boolean handleMessage(final Message msg) {
        if (msg.obj != null) {
            final EventSender sender = (EventSender) msg.obj;
            sender.send();
        }
        return true;
    }

    @Override
    public void post(final EventSender sender) {
        final Message message = mHandler.obtainMessage(0, sender);
        mHandler.sendMessage(message);
    }
}
