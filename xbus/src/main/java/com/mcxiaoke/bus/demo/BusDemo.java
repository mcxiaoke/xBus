package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.BusReceiver;

/**
 * User: mcxiaoke
 * Date: 15/8/3
 * Time: 10:59
 */
public class BusDemo {

    public static void main(String[] args) {
        final BusDemo demo = new BusDemo();
        Bus bus = Bus.getDefault();
        bus.register(demo);
        bus.post(new Object());
        bus.post("SomeEvent");
        bus.post(12345);
        bus.post(new RuntimeException("Error"));
    }

    @BusReceiver
    public void onReceiveRunnableNotPost(Runnable event) {
        System.out.println("onReceiveRunnableNotPost() event=" + event);
    }

    @BusReceiver
    public void onObjectEvent(Object event) {
        System.out.println("onObjectReceive() event=" + event);
    }

    @BusReceiver
    public void onExceptionEvent(Exception event) {
        System.out.println("onExceptionEvent() event=" + event);
    }

    @BusReceiver
    public void onStringReceive(String event) {
        System.out.println("onStringReceive() event=" + event);
    }

    @BusReceiver
    public void onInteger(Integer event) {
        System.out.println("onInteger() event=" + event);
    }
}
