package com.mcxiaoke.bus.scheduler;

import com.mcxiaoke.bus.EventSender;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:36
 */
public interface Scheduler {

    void post(EventSender sender);
}
