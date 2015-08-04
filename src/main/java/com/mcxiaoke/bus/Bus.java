package com.mcxiaoke.bus;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * User: mcxiaoke
 * Date: 15/7/30
 * Time: 18:09
 */
public class Bus {

    static class Subscriber {
        public final Method method;
        public final Object target;
        public final Class<?> targetType;
        public final Class<?> eventType;

        public Subscriber(final Method method, final Object target) {
            this.method = method;
            this.target = target;
            this.eventType = method.getParameterTypes()[0];
            this.targetType = target.getClass();
        }

        public boolean match(final Class<?> eventClass) {
            return eventType.isAssignableFrom(eventClass);
        }

        public Object invoke(Object event) {
            try {
                return method.invoke(target, event);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        @Override
        public String toString() {
            return targetType.getName() + "." + method.getName()
                    + "(" + eventType.getSimpleName() + ")";
        }
    }

    private static class SingletonHolder {
        static final Bus INSTANCE = new Bus();
    }

    public static Bus getDefault() {
        return SingletonHolder.INSTANCE;
    }

    private Map<Object, Set<Subscriber>> mTargetMap;
    private Map<Class<?>, Set<Subscriber>> mSubscriberCacheMap;

    private Bus() {
        mTargetMap = new WeakHashMap<Object, Set<Subscriber>>();
        mSubscriberCacheMap = new HashMap<Class<?>, Set<Subscriber>>();
    }

    public void register(final Object target) {
        if (mTargetMap.containsKey(target)) {
            System.err.println("target " + target + " is already registered");
            return;
        }
        Set<Subscriber> subscribers = Helper.findSubscriber(target, BusReceiver.class);
        if (subscribers == null || subscribers.isEmpty()) {
            return;
        }
        mTargetMap.put(target, subscribers);
    }

    public void unregister(final Object target) {
        System.out.println("unregister() target=" + target);
        mTargetMap.remove(target);
    }

    public void post(Object event) {
        final Class<?> eventClass = event.getClass();
        Set<Subscriber> cache = mSubscriberCacheMap.get(eventClass);
        if (cache == null) {
            System.out.println("post() subscribers no cache, do query");
            cache = new HashSet<Subscriber>();
            mSubscriberCacheMap.put(eventClass, cache);
            for (Map.Entry<Object, Set<Subscriber>> entry : mTargetMap.entrySet()) {
                final Object target = entry.getKey();
                final Set<Subscriber> subscriberSet = entry.getValue();
                if (subscriberSet == null || subscriberSet.isEmpty()) {
                    continue;
                }
                for (Subscriber subscriber : subscriberSet) {
                    if (subscriber.match(eventClass)) {
                        cache.add(subscriber);
                    }
                }
            }
        } else {
            System.out.println("post() subscribers from cache, size=" + cache.size());
        }
        final Set<Subscriber> subscribers = new HashSet<Subscriber>(cache);
        int sentCount = 0;
        for (Subscriber subscriber : subscribers) {
            if (subscriber.match(eventClass)) {
                System.out.println("post() event to " + subscriber);
                subscriber.invoke(event);
                sentCount++;
                cache.add(subscriber);
            }
        }
        if (sentCount == 0) {
            System.err.println("post() no receiver found for event: " + event);
        }
    }

}
