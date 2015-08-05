package com.mcxiaoke.bus.scheduler;

import android.os.Handler;

import java.util.concurrent.Executor;

/**
 * User: mcxiaoke
 * Date: 15/8/5
 * Time: 12:01
 */
public class HandlerExecutor implements Executor {
    private final Handler handler;

    public HandlerExecutor(final Handler handler) {
        this.handler = handler;
    }

    @Override
    public void execute(Runnable runnable) {
        handler.post(runnable);
    }
}
