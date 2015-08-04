package com.mcxiaoke.bus.demo;

/**
 * User: mcxiaoke
 * Date: 15/8/3
 * Time: 18:06
 */

import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.BusReceiver;

import java.awt.*;

/**
 * Output:
 * onCharSequenceEvent() event=A StringBuilder
 * onObjectEvent() event=A StringBuilder
 */
public class EventDemo extends Component {

    public static void main(String[] args) {
        new EventDemo().run();
    }

    public void run() {
        long start = System.nanoTime();
        Bus.getDefault().post(new StringBuilder("Before Unregister"));
        Bus.getDefault().register(this);
        Bus.getDefault().post(new StringBuilder("A StringBuilder"));
        for (int i = 0; i < 5; i++) {
            Bus.getDefault().post(new StringBuilder("A StringBuilder +" + i));
        }
        long end = System.nanoTime();
        System.out.println("elapsed time: " + (end - start) / 1000000 + "ms");
        Bus.getDefault().unregister(this);
//        Bus.getDefault().post(new StringBuilder("After Unregister"));
        Bus.getDefault().dump();
    }

    @BusReceiver
    public void onStringEvent(String event) {
        // 不会执行，因为event是StringBuilder，event instanceof String == false
        System.out.println("onStringEvent() event=" + event);
    }

    @BusReceiver
    public void onExceptionEvent(Exception event) {
        // 不会执行，因为event是StringBuilder，event instanceof Exception == false
        System.out.println("onExceptionEvent() event=" + event);
    }

    @BusReceiver
    public void onCharSequenceEvent(CharSequence event) {
        // 会执行，因为event是StringBuilder，event instanceof CharSequence == true
        System.out.println("onCharSequenceEvent() event=" + event);
    }

    @BusReceiver
    public void onObjectEvent(Object event) {
        // 会执行，因为event是StringBuilder，event instanceof Object == true
        System.out.println("onObjectEvent() event=" + event);
    }
}
