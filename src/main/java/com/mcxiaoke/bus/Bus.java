package com.mcxiaoke.bus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

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

    private Map<Object, Set<Method>> mMethodMap = new WeakHashMap<Object, Set<Method>>();

    public void register(final Object target) {
        if (mMethodMap.containsKey(target)) {
            System.err.println("target " + target + " is already registered");
            return;
        }
        Set<Method> methods = Helper.findAnnotatedMethods(target.getClass(), BusReceiver.class);
        if (methods == null || methods.isEmpty()) {
            return;
        }
        mMethodMap.put(target, methods);
    }

    public void unregister(final Object target) {
        System.out.println("unregister() target=" + target);
        mMethodMap.remove(target);
    }

    public void post(Object event) {
        final Class<?> eventClass = event.getClass();
        int sentCount = 0;
        for (Map.Entry<Object, Set<Method>> entry : mMethodMap.entrySet()) {
            final Object target = entry.getKey();
            final Set<Method> methods = entry.getValue();
            if (methods == null || methods.isEmpty()) {
                continue;
            }
            for (Method method : methods) {
                Class<?> parameterClass = method.getParameterTypes()[0];
                if (parameterClass.isAssignableFrom(eventClass)) {
                    try {
                        System.out.println("post event to "
                                + target + "." + method.getName());
                        method.invoke(target, event);
                        sentCount++;
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if (sentCount == 0) {
            System.err.println("no receiver found for event: " + event);
        }
    }

}
