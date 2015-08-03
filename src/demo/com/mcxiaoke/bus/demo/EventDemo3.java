package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.BusReceiver;

/**
 * User: mcxiaoke
 * Date: 15/7/31
 * Time: 14:04
 */
public class EventDemo3 extends BaseEventDemo {


    @BusReceiver
    public void onReceive1(SomeEvent1 event) {
        System.out.println("EventDemo3.onReceive1() event=" + event);
    }

    @BusReceiver
    public void onReceive2(SomeEvent2 event) {
        System.out.println("EventDemo3.onReceive2() event=" + event);
    }

    @BusReceiver
    public void onReceive3(SomeEvent4 event) {
        System.out.println("EventDemo3.onReceive3() event=" + event);
    }

    @BusReceiver
    public void onDemo0(BaseDemoEvent event) {
        System.out.println("onDemo0() event=" + event
                + " class=" + this.getClass().getSimpleName());
    }

    @BusReceiver
    public void onDemo1(BaseDemoEvent event) {
        System.out.println("onDemo1() event=" + event
                + " class=" + this.getClass().getSimpleName());
    }

    @BusReceiver
    public void onDemo2(IDemoEvent event) {
        System.out.println("onDemo2() event=" + event
                + " class=" + this.getClass().getSimpleName());
    }

    @BusReceiver
    public void onDemo3(IDemoEvent event) {
        System.out.println("onDemo3() event=" + event
                + " class=" + this.getClass().getSimpleName());
    }
}
