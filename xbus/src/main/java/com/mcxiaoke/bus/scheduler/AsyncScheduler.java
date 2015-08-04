package com.mcxiaoke.bus.scheduler;

import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.EventSender;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 16:04
 */
class AsyncScheduler implements Scheduler {
    private Bus mBus;
    private ExecutorService mExecutor;

    public AsyncScheduler(final Bus bus) {
        mBus = bus;
        mExecutor = Executors.newCachedThreadPool();
    }

    @Override
    public void post(final EventSender sender) {
        mExecutor.execute(new Runnable() {
            @Override
            public void run() {
                sender.send();
            }
        });
    }
}
