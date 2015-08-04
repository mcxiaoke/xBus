package com.mcxiaoke.bus;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * User: mcxiaoke
 * Date: 15/7/30
 * Time: 18:09
 */
public class Bus {

    static class MethodInfo {
        public final Method method;
        public final Class<?> targetType;
        public final Class<?> eventType;
        public final String name;

        public MethodInfo(final Method method, final Class<?> targetClass) {
            this.method = method;
            this.targetType = targetClass;
            this.eventType = method.getParameterTypes()[0];
            this.name = targetType.getName() + "." + method.getName()
                    + "(" + eventType.getName() + ")";
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            final MethodInfo that = (MethodInfo) o;
            return name.equals(that.name);

        }

        @Override
        public int hashCode() {
            return name.hashCode();
        }
    }

    static class EventSender {
        public final Object event;
        public final Subscriber subscriber;
        public final Class<?> eventType;

        public EventSender(final Object event, final Subscriber subscriber) {
            this.event = event;
            this.subscriber = subscriber;
            this.eventType = event.getClass();
        }

        public Object send() {
            try {
                System.out.println("send event:[" + eventType.getSimpleName()
                        + "] to subscriber:[" + subscriber.targetType.getSimpleName() + "]");
                return subscriber.invoke(event);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    static class Subscriber {
        public final MethodInfo method;
        public final Object target;
        public final Class<?> targetType;
        public final Class<?> eventType;
        public final String name;

        public Subscriber(final MethodInfo method, final Object target) {
            this.method = method;
            this.target = target;
            this.eventType = method.eventType;
            this.targetType = target.getClass();
            this.name = method.name;
        }

        public Object invoke(Object event) {
            try {
                return this.method.method.invoke(this.target, event);
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }

        public boolean match(final Class<?> eventClass) {
            return this.eventType.isAssignableFrom(eventClass);
        }

        @Override
        public String toString() {
            return targetType.getSimpleName() + "."
                    + method.method.getName()
                    + "(" + eventType.getSimpleName() + ")";
        }


    }

    private static class SingletonHolder {
        static final Bus INSTANCE = new Bus();
    }

    public static Bus getDefault() {
        return SingletonHolder.INSTANCE;
    }

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

    private Bus() {
        mMethodCache = new HashMap<String, Set<MethodInfo>>();
        mEventTypeCache = new HashMap<String, Set<Class<?>>>();
        mEventMap = new HashMap<Object, Set<Class<?>>>();
        mSubscriberMap = new HashMap<Class<?>, Set<Subscriber>>();
    }

    private Set<MethodInfo> getMethods(Class<?> targetClass) {
        String cacheKey = targetClass.getName();
        Set<MethodInfo> methods = mMethodCache.get(cacheKey);
        if (methods == null) {
            methods = Helper.findSubscriberMethods(targetClass);
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
        System.out.println("register() target=" + target);
        addSubscribers(target);
    }

    public void unregister(final Object target) {
        System.out.println("unregister() target=" + target);
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
                    System.out.println("unregister() remove " + subscriber);
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
                    new EventSender(event, subscriber).send();
                }
            }
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
        System.out.println("----------------------------------");
        System.out.println("<Target-Events>");
        for (Map.Entry<Object, Set<Class<?>>> entry : mEventMap.entrySet()) {
            System.out.println("  Target:" + entry.getKey().getClass().getSimpleName());
            Set<Class<?>> eventTypes = entry.getValue();
            for (Class<?> eventType : eventTypes) {
                System.out.println("    EventType:" + eventType.getSimpleName());
            }
        }
        System.out.println("<Event-Subscribers>");
        for (Map.Entry<Class<?>, Set<Subscriber>> entry : mSubscriberMap.entrySet()) {
            System.out.println("  EventType:" + entry.getKey().getSimpleName());
            Set<Subscriber> subscribers = entry.getValue();
            for (Subscriber subscriber : subscribers) {
                System.out.println("    Subscriber:" + subscriber);
            }
        }
        System.out.println("----------------------------------");
    }

}
