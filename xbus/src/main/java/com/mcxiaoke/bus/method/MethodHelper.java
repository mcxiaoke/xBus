package com.mcxiaoke.bus.method;

import com.mcxiaoke.bus.annotation.BusReceiver;
import com.mcxiaoke.bus.Bus.EventMode;
import com.mcxiaoke.bus.MethodInfo;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 18:16
 */
public class MethodHelper {
    public static boolean shouldSkipClass(final Class<?> clazz) {
        final String clsName = clazz.getName();
        return Object.class.equals(clazz)
                || clsName.startsWith("java.")
                || clsName.startsWith("javax.")
                || clsName.startsWith("android.")
                || clsName.startsWith("com.android.");
    }

    public static boolean isValidMethod(final Method method) {
//        if (!Modifier.isPublic(method.getModifiers())) {
//            return false;
//        }
        // must not static
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        // must not be private
        if (Modifier.isPrivate(method.getModifiers())) {
            return false;
        }
        // must not be volatile
        // fix getDeclaredMethods bug, if method in base class,
        // it returns duplicate method,
        // one is normal, the other is the same but with volatile modifier
        if (Modifier.isVolatile(method.getModifiers())) {
            return false;
        }
        // must has only one parameter
        if (method.getParameterTypes().length != 1) {
            return false;
        }

        return true;
    }

    public static Set<MethodInfo> findSubscriberMethods(
            final Class<?> targetClass, MethodConverter converter) {
        Class<?> clazz = targetClass;
        final Set<MethodInfo> methods = new HashSet<MethodInfo>();
        while (!shouldSkipClass(clazz)) {
            final Method[] clsMethods = clazz.getDeclaredMethods();
            System.out.println("findSubscriberMethods() " + clazz.getSimpleName()
                    + " has " + clsMethods.length + " methods");
            for (final Method method : clsMethods) {
                final MethodInfo methodInfo = converter.convert(method);
                if (methodInfo != null) {
                    methods.add(methodInfo);
                }
            }
            // search more methods in super class
            clazz = clazz.getSuperclass();
        }
        return methods;
    }

    public static Set<MethodInfo> findSubscriberMethodsByAnnotation(
            final Class<?> targetClass) {
        final MethodConverter converter = new MethodConverter() {
            @Override
            public MethodInfo convert(final Method method) {
                // check annotation
                if (!method.isAnnotationPresent(BusReceiver.class)) {
                    return null;
                }
                if (!isValidMethod(method)) {
                    return null;
                }
                BusReceiver annotation = method.getAnnotation(BusReceiver.class);
                return new MethodInfo(method, targetClass, annotation.mode());
            }
        };
        return findSubscriberMethods(targetClass, converter);
    }

    public static Set<MethodInfo> findSubscriberMethodsByName(
            final Class<?> targetClass, final String name) {
        final MethodConverter converter = new MethodConverter() {
            @Override
            public MethodInfo convert(final Method method) {
                // check name
                if (!name.equals(method.getName())) {
                    return null;
                }
                if (!isValidMethod(method)) {
                    return null;
                }
                return new MethodInfo(method, targetClass, EventMode.Main);
            }
        };
        return findSubscriberMethods(targetClass, converter);
    }
}
