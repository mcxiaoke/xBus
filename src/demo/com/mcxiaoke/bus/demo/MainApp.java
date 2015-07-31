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
        bus.register(demo1);
        bus.register(demo2);
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
    }


}
