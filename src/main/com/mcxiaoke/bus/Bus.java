package com.mcxiaoke.bus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
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

    private Map<Object, List<Method>> mMethodMap = new WeakHashMap<Object, List<Method>>();

    public void register(final Object target) {
        List<Method> methods = Helper.findAnnotatedMethods(target.getClass(), BusReceiver.class);
        if (methods == null || methods.isEmpty()) {
            return;
        }
        mMethodMap.put(target, methods);
    }

    public void unregister(final Object target) {
        mMethodMap.remove(target);
    }

    public void post(Object event) {
        final Class<?> eventClass = event.getClass();
        for (Map.Entry<Object, List<Method>> entry : mMethodMap.entrySet()) {
            final Object target = entry.getKey();
            final List<Method> methods = entry.getValue();
            if (methods == null || methods.isEmpty()) {
                continue;
            }
            for (Method method : methods) {
                if (eventClass.equals(method.getParameterTypes()[0])) {
                    try {
                        method.invoke(target, event);
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

}
