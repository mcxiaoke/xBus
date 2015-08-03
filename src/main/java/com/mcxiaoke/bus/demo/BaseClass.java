package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.Bus;

/**
 * User: mcxiaoke
 * Date: 15/8/3
 * Time: 18:00
 */
public class BaseClass {

    void onCreate() {
        Bus.getDefault().register(this);
    }

    void onDestroy() {
        Bus.getDefault().unregister(this);
    }
}
