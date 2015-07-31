package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.BusReceiver;

/**
 * User: mcxiaoke
 * Date: 15/7/31
 * Time: 16:10
 */
public class EventDemo1 {

    @BusReceiver
    public void onReceive(SomeEvent1 event) {
        System.out.println("EventDemo1.onReceive() event=" + event);
    }

    @BusReceiver
    public void onReceive2(SomeEvent2 event) {
        System.out.println("EventDemo1.onReceive2() event=" + event);
    }

    @BusReceiver
    public void onReceive3(SomeEvent3 event) {
        System.out.println("EventDemo1.onReceive3() event=" + event);
    }
}
