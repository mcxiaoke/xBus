package com.mcxiaoke.bus;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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

    private Map<Object, List<Method>> mMethodMap = new HashMap<Object, List<Method>>();
    private Map<Class<?>, Set<Object>> mTargetMap = new HashMap<Class<?>, Set<Object>>();

    public boolean register(final Object target) {
        List<Method> methods = Helper.findAnnotatedMethods(target.getClass(), BusReceiver.class);
        if (methods == null || methods.isEmpty()) {
            return false;
        }
        System.out.println("register target=" + target);
        mMethodMap.put(target, methods);
        for (Method method : methods) {
            final Class<?> parameterClass = method.getParameterTypes()[0];
            Set<Object> targets = mTargetMap.get(parameterClass);
            if (targets == null) {
                targets = new HashSet<Object>();
                mTargetMap.put(parameterClass, targets);
            }
            targets.add(target);
        }
        return true;
    }

    public boolean unregister(final Object target) {
        System.out.println("unregister target=" + target);
        mMethodMap.remove(target);
        for (Map.Entry<Class<?>, Set<Object>> entry : mTargetMap.entrySet()) {
            Set<Object> objects = entry.getValue();
            if (objects == null || objects.isEmpty()) {
                continue;
            }
            objects.remove(target);
        }
        return false;
    }

    public void post(Object event) {
        System.out.println("post event=" + event);
        Class<?> eventClass = event.getClass();
        Set<Object> objects = mTargetMap.get(eventClass);
        if (objects == null || objects.isEmpty()) {
            return;
        }
        for (Object object : objects) {
            List<Method> methods = mMethodMap.get(object);
            if (methods == null || methods.isEmpty()) {
                continue;
            }
            for (Method method : methods) {
                if (eventClass.equals(method.getParameterTypes()[0])) {
                    try {
                        method.invoke(object, event);
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
