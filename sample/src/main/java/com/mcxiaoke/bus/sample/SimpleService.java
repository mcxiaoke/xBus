package com.mcxiaoke.bus.sample;

import android.app.IntentService;
import android.content.Intent;
import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.sample.SimpleActivity.SomeEventClass;

/**
 * User: mcxiaoke
 * Date: 15/8/7
 * Time: 14:11
 */
public class SimpleService extends IntentService {

    public SimpleService() {
        super("SimpleService");
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
       // 这里发送事件
        Bus.getDefault().post("String Event");
        Bus.getDefault().post(new SomeEventClass());
        Bus.getDefault().post(new Object());
    }
}
