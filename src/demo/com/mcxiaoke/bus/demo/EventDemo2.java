package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.BusReceiver;

/**
 * User: mcxiaoke
 * Date: 15/7/31
 * Time: 14:04
 */
public class EventDemo2 {


    @BusReceiver
    public void onReceive1(SomeEvent1 event) {
        System.out.println("EventDemo2.onReceive1() event=" + event);
    }

    @BusReceiver
    public void onReceive2(SomeEvent2 event) {
        System.out.println("EventDemo2.onReceive2() event=" + event);
    }

    @BusReceiver
    public void onReceive3(SomeEvent3 event) {
        System.out.println("EventDemo2.onReceive3() event=" + event);
    }
}
