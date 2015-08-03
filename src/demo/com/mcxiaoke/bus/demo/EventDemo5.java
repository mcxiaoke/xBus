package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.BusReceiver;

/**
 * User: mcxiaoke
 * Date: 15/7/31
 * Time: 14:04
 */
public class EventDemo5 extends BaseEventDemo {

    @BusReceiver
    public void onReceive1(SomeEvent1 event) {
        System.out.println("EventDemo5.onReceive1() event=" + event);
    }

    @Override
    public void onBaseReceive1(final SomeEvent1 event) {
        super.onBaseReceive1(event);
    }

    @Override
    public void onBaseReceive2(final SomeEvent2 event) {
        super.onBaseReceive2(event);
    }
}
