package com.mcxiaoke.bus.demo;

/**
 * User: mcxiaoke
 * Date: 15/8/3
 * Time: 13:37
 */
public class BaseDemoEvent implements IDemoEvent {

    @Override
    public String toString() {
        return BaseDemoEvent.class.getSimpleName();
    }
}
