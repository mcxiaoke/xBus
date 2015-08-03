package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.BusReceiver;

/**
 * User: mcxiaoke
 * Date: 15/7/31
 * Time: 16:10
 */
public class BaseEventDemo {

    public void start(Bus bus){
       bus.register(this);
    }

    public void stop(Bus bus){
      bus.unregister(this);
    }

    @BusReceiver
    public void onBaseReceive1(SomeEvent1 event) {
        System.out.println("onBaseReceive1() event=" + event
                + " class=" + this.getClass().getSimpleName());
    }

    @BusReceiver
    public void onBaseReceive2(SomeEvent2 event) {
        System.out.println("onBaseReceive2() event=" + event
                + " class=" + this.getClass().getSimpleName());
    }

    @BusReceiver
    public void onBaseReceive3(SomeEvent3 event) {
        System.out.println("onBaseReceive3() event=" + event
                + " class=" + this.getClass().getSimpleName());
    }
}
