package com.mcxiaoke.bus.scheduler;

import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.EventSender;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 16:00
 */
class SenderScheduler implements Scheduler {
    private Bus mBus;

    public SenderScheduler(final Bus bus) {
        mBus = bus;
    }

    @Override
    public void post(final EventSender sender) {
        sender.send();
    }
}
