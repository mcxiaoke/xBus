package com.mcxiaoke.bus;

import android.util.Log;
import com.mcxiaoke.bus.method.AnnotationMethodFinder;
import com.mcxiaoke.bus.method.MethodFinder;
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
    private final Map<Class<?>, Object> mStickyEventMap;

    private MethodFinder mMethodFinder;
    private boolean mStrictMode;

    private Scheduler mMainScheduler;
    private Scheduler mSenderScheduler;
    private Scheduler mThreadScheduler;

    private StopWatch mStopWatch;
    private volatile boolean mDebug;

    private Bus() {
        mEventMap = new ConcurrentHashMap<Object, Set<Class<?>>>();
        mSubscriberMap = new ConcurrentHashMap<Class<?>, Set<Subscriber>>();
        mStickyEventMap = new ConcurrentHashMap<Class<?>, Object>();
        mMethodFinder = new AnnotationMethodFinder();
        mStrictMode = false;
        mMainScheduler = Schedulers.main(this);
        mSenderScheduler = Schedulers.sender(this);
        mThreadScheduler = Schedulers.thread(this);
        mStopWatch = new StopWatch(TAG);
    }

    public Bus setDebug(final boolean debug) {
        mDebug = debug;
        return this;
    }

    public Bus setMethodFinder(final MethodFinder finder) {
        mMethodFinder = finder;
        return this;
    }

    public Bus setStrictMode(final boolean strictMode) {
        mStrictMode = strictMode;
        return this;
    }

    public boolean isStrictMode() {
        return mStrictMode;
    }

    private Set<MethodInfo> getMethods(Class<?> targetClass) {
        String cacheKey = targetClass.getName();
        Set<MethodInfo> methods;
        synchronized (Cache.sMethodCache) {
            methods = Cache.sMethodCache.get(cacheKey);
        }
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
            addSubscriber(subscriber);
            checkStickyEvent(subscriber);
        }
    }

    private synchronized void addSubscriber(final Subscriber subscriber) {
        Set<Subscriber> ss = mSubscriberMap.get(subscriber.eventType);
        if (ss == null) {
            ss = new HashSet<Subscriber>();
            mSubscriberMap.put(subscriber.eventType, ss);
        }
        // 将subscriber添加到eventType对应的订阅者集合里
        ss.add(subscriber);
    }

    private void checkStickyEvent(final Subscriber subscriber) {
        final Class<?> eventType = subscriber.eventType;
        // one sticky event per once event type
        final Object event = mStickyEventMap.get(eventType);
        if (event != null) {
            sendEvent(new EventEmitter(this, event, subscriber, mDebug));
        }
    }

    public <T> void register(final T target) {
        if (mDebug) {
            Log.v(TAG, "register() target:[" + target + "]");
            mStopWatch.start("register()");
        }
        addSubscribers(target);
        if (mDebug) {
            mStopWatch.stop("register()");
        }
    }

    public <T> void unregister(final T target) {
        if (mDebug) {
            Log.v(TAG, "unregister() target:" + target);
            mStopWatch.start("unregister()");
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
        if (mDebug) {
            mStopWatch.stop("unregister()");
        }
    }

    public <E> void postSticky(E event) {
        post(event);
        synchronized (mStickyEventMap) {
            mStickyEventMap.put(event.getClass(), event);
        }
    }

    public <E> void post(E event) {
        final Class<?> theEventType = event.getClass();
        if (mDebug) {
            Log.v(TAG, "post() event:" + event + " type:"
                    + theEventType.getSimpleName());
            mStopWatch.start("post() " + theEventType.getSimpleName());
        }
        // strict mode, no subclass match
        // rule: event.getClass().equals(parameterClass)
        // parameterClass must be the same as event.getClass()
        // eg.
        // event type = String
        // onEvent(Exception event) - not matched
        // onEvent(String event) - matched
        // onEvent(CharSequence event) - not matched
        // onEvent(Object event) - not matched
        if (mStrictMode) {
            postEventByType(event, theEventType);
            return;
        }
        // default policy, subclass match,eg.
        // rule: parameterClass.isAssignableFrom(event.getClass())
        // parameterClass must be super class or interface of event.getClass()
        // eg.
        // event type = String
        // onEvent(Exception event) - not matched
        // onEvent(String event) - matched
        // onEvent(CharSequence event) - matched
        // onEvent(Object event) - matched
        final String cacheKey = theEventType.getName();
        Set<Class<?>> eventTypes;
        synchronized (Cache.sEventTypeCache) {
            eventTypes = Cache.sEventTypeCache.get(cacheKey);
        }
        if (eventTypes == null) {
            eventTypes = Helper.findSuperTypes(theEventType);
            if (mDebug) {
                Log.v(TAG, "post() no event type cache, find types");
            }
            synchronized (Cache.sEventTypeCache) {
                Cache.sEventTypeCache.put(cacheKey, eventTypes);
            }
        }
        for (Class<?> eventType : eventTypes) {
            postEventByType(event, eventType);
        }
        if (mDebug) {
            mStopWatch.stop("post() " + theEventType.getSimpleName());
        }
    }

    private <E> void postEventByType(final E event, final Class<?> eventType) {
        final Set<Subscriber> subscribers = mSubscriberMap.get(eventType);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        for (Subscriber subscriber : subscribers) {
            sendEvent(new EventEmitter(this, event, subscriber, mDebug));
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
