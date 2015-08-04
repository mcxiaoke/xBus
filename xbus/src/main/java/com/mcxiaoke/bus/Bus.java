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

    private static class SingletonHolder {
        static final Bus INSTANCE = new Bus();
    }

    public static Bus getDefault() {
        return SingletonHolder.INSTANCE;
    }

    private static final String TAG = Bus.class.getSimpleName();
    private static final boolean DEBUG = BuildConfig.DEBUG;

    // key=注册类的完整类名 target.getClass().getName()
    // value=注册类包含的合法的@BusReceiver方法对象集合
    // targetTypeName->method set
    private final Map<String, Set<MethodInfo>> mMethodCache;
    // key=事件类型的完整类名
    // value=事件类型的所有父类和接口
    // eventTypeName-> event type set
    private final Map<String, Set<Class<?>>> mEventTypeCache;
    // key=注册类的对象
    // value=注册类包含的事件类型集合
    // target->eventType set
    private Map<Object, Set<Class<?>>> mEventMap;
    // key=事件参数类型
    // value=事件参数所属的Subscriber对象集合
    // eventType->subscriber set
    private Map<Class<?>, Set<Subscriber>> mSubscriberMap;

    private MethodFinder mMethodFinder;

    private Scheduler mMainScheduler;
    private Scheduler mSenderScheduler;
    private Scheduler mThreadScheduler;

    private Bus() {
        mMethodCache = new ConcurrentHashMap<String, Set<MethodInfo>>();
        mEventTypeCache = new ConcurrentHashMap<String, Set<Class<?>>>();
        mEventMap = new ConcurrentHashMap<Object, Set<Class<?>>>();
        mSubscriberMap = new ConcurrentHashMap<Class<?>, Set<Subscriber>>();
        mMethodFinder = new AnnotationMethodFinder();
        mMainScheduler = Schedulers.main(this);
        mSenderScheduler = Schedulers.sender(this);
        mThreadScheduler = Schedulers.thread(this);
    }

    public MethodFinder getMethodFinder() {
        return mMethodFinder;
    }

    public void setMethodFinder(final MethodFinder finder) {
        mMethodFinder = finder;
    }

    private Set<MethodInfo> getMethods(Class<?> targetClass) {
        String cacheKey = targetClass.getName();
        Set<MethodInfo> methods = mMethodCache.get(cacheKey);
        if (methods == null) {
            methods = mMethodFinder.findSubscriberMethods(targetClass);
            mMethodCache.put(cacheKey, methods);
        }
        return methods;
    }

    private void addSubscribers(final Object target) {
        Set<Class<?>> eventTypes = mEventMap.get(target);
        if (eventTypes == null) {
            eventTypes = new HashSet<Class<?>>();
            mEventMap.put(target, eventTypes);
        }
        Set<MethodInfo> methods = getMethods(target.getClass());
        for (MethodInfo method : methods) {
            final Subscriber subscriber = new Subscriber(method, target);
            eventTypes.add(subscriber.eventType);
            Set<Subscriber> ss = mSubscriberMap.get(subscriber.eventType);
            if (ss == null) {
                ss = new HashSet<Subscriber>();
                mSubscriberMap.put(subscriber.eventType, ss);
            }
            ss.add(subscriber);
        }
    }

    public void register(final Object target) {
        Log.v(TAG, "register() target=" + target);
        addSubscribers(target);
    }

    public void unregister(final Object target) {
        Log.v(TAG, "unregister() target=" + target);
        final Set<Class<?>> eventTypes = mEventMap.remove(target);
        for (Class<?> eventType : eventTypes) {
            Set<Subscriber> subscribers = mSubscriberMap.get(eventType);
            if (subscribers == null || subscribers.isEmpty()) {
                continue;
            }
            Iterator<Subscriber> it = subscribers.iterator();
            while (it.hasNext()) {
                final Subscriber subscriber = it.next();
                if (subscriber.target == target) {
                    it.remove();
                    Log.v(TAG, "unregister() remove " + subscriber);
                }
            }
        }
    }

    public void post(Object event) {
        final Class<?> theEventType = event.getClass();
        final String cacheKey = theEventType.getName();
        Set<Class<?>> eventTypes = mEventTypeCache.get(cacheKey);
        if (eventTypes == null) {
            eventTypes = Helper.findSuperTypes(theEventType);
            mEventTypeCache.put(cacheKey, eventTypes);
        }
        for (Class<?> eventType : eventTypes) {
            final Set<Subscriber> subscribers = mSubscriberMap.get(eventType);
            if (subscribers == null || subscribers.isEmpty()) {
                continue;
            }
            for (Subscriber subscriber : subscribers) {
                if (subscriber.match(eventType)) {
                    sendEvent(event, subscriber);
                }
            }
        }
    }

    public void sendEvent(final Object event, Subscriber subscriber) {
        Log.v(TAG, "sendEvent event=" + event + " subscriber=" + subscriber);
        final EventSender sender = new EventSender(event, subscriber);
        if (EventMode.Sender.equals(subscriber.mode)) {
            mSenderScheduler.post(sender);
        } else if (EventMode.Main.equals(subscriber.mode)) {
            mMainScheduler.post(sender);
        } else if (EventMode.Thread.equals(subscriber.mode)) {
            mThreadScheduler.post(sender);
        }
    }

    // for debug only
    public void reset() {
        mMethodCache.clear();
        mEventTypeCache.clear();
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
