package com.mcxiaoke.bus.scheduler;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:36
 */
public interface Scheduler {

    void post(Runnable runnable);
}
