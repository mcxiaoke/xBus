package com.mcxiaoke.bus.demo;

import com.mcxiaoke.bus.Bus;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * User: mcxiaoke
 * Date: 15/7/31
 * Time: 14:04
 */
public class MainApp {

    public static void main(String[] args) {
        try {
            new MainApp().demo();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void demo() throws Exception {
        final ExecutorService mExecutor = Executors.newCachedThreadPool();
        final Bus bus = Bus.getDefault();

        final EventDemo1 demo1 = new EventDemo1();
        final EventDemo2 demo2 = new EventDemo2();
        final BaseEventDemo demo3 = new EventDemo3();
        final BaseEventDemo demo4 = new EventDemo4();
        final BaseEventDemo demo5 = new EventDemo5();
        bus.register(demo1);
        bus.register(demo2);
        bus.register(demo3);
        demo3.start(bus);
        demo4.start(bus);
        demo5.start(bus);
        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mExecutor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    bus.post(new SomeEvent1());
                    Thread.sleep(500);
                    bus.post(new SomeEvent3());
                    Thread.sleep(500);
                    bus.post(new SomeEvent5());
                    Thread.sleep(500);
                    bus.post(new SomeEvent2());
                    Thread.sleep(500);
                    bus.post(new SomeEvent4());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    countDownLatch.countDown();
                }

            }
        });
        countDownLatch.await();
        bus.unregister(demo1);
        bus.unregister(demo2);
        bus.unregister(demo3);
        demo4.stop(bus);
        demo5.stop(bus);
    }


}
