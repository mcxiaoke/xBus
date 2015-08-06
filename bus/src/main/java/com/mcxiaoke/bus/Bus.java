package com.mcxiaoke.bus;

import android.util.Log;
import com.mcxiaoke.bus.method.AnnotationMethodFinder;
import com.mcxiaoke.bus.method.MethodFinder;
import com.mcxiaoke.bus.method.NamedMethodFinder;
import com.mcxiaoke.bus.scheduler.Scheduler;
import com.mcxiaoke.bus.scheduler.Schedulers;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * User: mcxiaoke
 * Date: 15/7/30
 * Time: 18:09
 */
public class Bus {

    public enum EventMode {

        Sender, Main, Thread
    }

    static class Cache {

        // key=注册类的完整类名 target.getClass().getName()
        // value=注册类包含的合法的@BusReceiver方法对象集合
        // targetTypeName->method set
        final static Map<String, Set<MethodInfo>> sMethodCache =
                new ConcurrentHashMap<String, Set<MethodInfo>>();
        // key=事件类型的完整类名
        // value=事件类型的所有父类和接口
        // eventTypeName-> event type set
        final static Map<String, Set<Class<?>>> sEventTypeCache =
                new ConcurrentHashMap<String, Set<Class<?>>>();


    }

    private static class SingletonHolder {
        static final Bus INSTANCE = new Bus();
    }

    public static Bus getDefault() {
        return SingletonHolder.INSTANCE;
    }

    public static final String TAG = Bus.class.getSimpleName();

    // key=注册类的对象
    // value=注册类包含的事件类型集合
    // target->eventType set
    private final Map<Object, Set<Class<?>>> mEventMap;
    // key=事件参数类型
    // value=事件参数所属的Subscriber对象集合
    // eventType->subscriber set
    private final Map<Class<?>, Set<Subscriber>> mSubscriberMap;

    private MethodFinder mMethodFinder;

    private Scheduler mMainScheduler;
    private Scheduler mSenderScheduler;
    private Scheduler mThreadScheduler;

    private volatile boolean mDebug;

    private Bus() {
        mEventMap = new ConcurrentHashMap<Object, Set<Class<?>>>();
        mSubscriberMap = new ConcurrentHashMap<Class<?>, Set<Subscriber>>();
        mMethodFinder = new AnnotationMethodFinder();
        mMainScheduler = Schedulers.main(this);
        mSenderScheduler = Schedulers.sender(this);
        mThreadScheduler = Schedulers.thread(this);
    }

    public Bus setDebug(final boolean debug) {
        mDebug = debug;
        return this;
    }

    public Bus setMethodFinder(final MethodFinder finder) {
        mMethodFinder = finder;
        return this;
    }

    public Bus setEventMethodName(final String methodName) {
        mMethodFinder = new NamedMethodFinder(methodName);
        return this;
    }

    private Set<MethodInfo> getMethods(Class<?> targetClass) {
        String cacheKey = targetClass.getName();
        Set<MethodInfo> methods = Cache.sMethodCache.get(cacheKey);
        if (methods == null) {
            methods = mMethodFinder.find(this, targetClass);
            synchronized (Cache.sMethodCache) {
                Cache.sMethodCache.put(cacheKey, methods);
            }
        }
        return methods;
    }

    private void addSubscribers(final Object target) {
        // 建立target-->eventType的对应关系
        // 每个target对象里可能有多个事件接收器方法，会订阅多个类型的event
        Class<?> targetType = target.getClass();
        Set<Class<?>> eventTypes = mEventMap.get(target);
        if (eventTypes == null) {
            eventTypes = new HashSet<Class<?>>();
            mEventMap.put(target, eventTypes);
        }
        // 这里找出target里包含的所有事件接收器方法
        Set<MethodInfo> methods = getMethods(targetType);
        for (MethodInfo method : methods) {
            final Subscriber subscriber = new Subscriber(method, target);
            // 将eventType，也就是参数类型添加到target对应的eventType集合里
            eventTypes.add(subscriber.eventType);
            synchronized (mSubscriberMap) {
                Set<Subscriber> ss = mSubscriberMap.get(subscriber.eventType);
                if (ss == null) {
                    ss = new HashSet<Subscriber>();
                    mSubscriberMap.put(subscriber.eventType, ss);
                }
                // 将subscriber添加到eventType对应的订阅者集合里
                ss.add(subscriber);
            }
        }
    }

    public <T> void register(final T target) {
        if (mDebug) {
            Log.v(TAG, "register() target:[" + target + "]");
        }
        addSubscribers(target);
    }

    public <T> void unregister(final T target) {
        if (mDebug) {
            Log.v(TAG, "unregister() target:" + target);
        }
        final Set<Class<?>> eventTypes = mEventMap.remove(target);
        for (Class<?> eventType : eventTypes) {
            Set<Subscriber> subscribers = mSubscriberMap.get(eventType);
            if (subscribers == null || subscribers.isEmpty()) {
                continue;
            }
            synchronized (mSubscriberMap) {
                Iterator<Subscriber> it = subscribers.iterator();
                while (it.hasNext()) {
                    final Subscriber subscriber = it.next();
                    if (subscriber.target == target) {
                        it.remove();
                        if (mDebug) {
                            Log.v(TAG, "unregister() remove subscriber:" + subscriber);
                        }
                    }
                }
            }
        }
    }

    public <E> void post(E event) {
        final Class<?> theEventType = event.getClass();
        if (mDebug) {
            Log.v(TAG, "post() event:" + event + " type:"
                    + theEventType.getSimpleName());
        }
        final String cacheKey = theEventType.getName();
        Set<Class<?>> eventTypes = Cache.sEventTypeCache.get(cacheKey);
        if (eventTypes == null) {
            eventTypes = Helper.findSuperTypes(theEventType);
            synchronized (Cache.sEventTypeCache) {
                Cache.sEventTypeCache.put(cacheKey, eventTypes);
            }
        }
        for (Class<?> eventType : eventTypes) {
            final Set<Subscriber> subscribers = mSubscriberMap.get(eventType);
            if (subscribers == null || subscribers.isEmpty()) {
                continue;
            }
            for (Subscriber subscriber : subscribers) {
                sendEvent(new EventEmitter(this, event, subscriber, mDebug));
            }
        }
    }

    public void sendEvent(EventEmitter emitter) {
        if (mDebug) {
            Log.v(TAG, "send event:" + emitter);
        }
        if (EventMode.Sender.equals(emitter.mode)) {
            mSenderScheduler.post(emitter);
        } else if (EventMode.Main.equals(emitter.mode)) {
            if (Helper.isMainThread()) {
                mSenderScheduler.post(emitter);
            } else {
                mMainScheduler.post(emitter);
            }
        } else if (EventMode.Thread.equals(emitter.mode)) {
            mThreadScheduler.post(emitter);
        }
    }

    // for debug only
    public void reset() {
        if (mDebug) {
            Log.v(TAG, "reset");
        }
        Cache.sMethodCache.clear();
        Cache.sEventTypeCache.clear();
        mEventMap.clear();
        mSubscriberMap.clear();
    }

    // for debug only
    public void dump() {
        Log.v(TAG, "----------------------------------");
        Log.v(TAG, "<Target-Events>");
        for (Map.Entry<Object, Set<Class<?>>> entry : mEventMap.entrySet()) {
            Log.v(TAG, "  Target:" + entry.getKey().getClass().getSimpleName());
            Set<Class<?>> eventTypes = entry.getValue();
            for (Class<?> eventType : eventTypes) {
                Log.v(TAG, "    EventType:" + eventType.getSimpleName());
            }
        }
        Log.v(TAG, "<Event-Subscribers>");
        for (Map.Entry<Class<?>, Set<Subscriber>> entry : mSubscriberMap.entrySet()) {
            Log.v(TAG, "  EventType:" + entry.getKey().getSimpleName());
            Set<Subscriber> subscribers = entry.getValue();
            for (Subscriber subscriber : subscribers) {
                Log.v(TAG, "    Subscriber:" + subscriber);
            }
        }
        Log.v(TAG, "----------------------------------");
    }

}
