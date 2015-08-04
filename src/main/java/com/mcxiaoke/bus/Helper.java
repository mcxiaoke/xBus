package com.mcxiaoke.bus;

import com.mcxiaoke.bus.Bus.Subscriber;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashSet;
import java.util.Set;

/**
 * User: mcxiaoke
 * Date: 15/7/31
 * Time: 13:22
 */
final class Helper {

    private static boolean shouldSkipClass(final Class<?> clazz) {
        final String clsName = clazz.getName();
        return Object.class.equals(clazz)
                || clsName.startsWith("java.")
                || clsName.startsWith("javax.")
                || clsName.startsWith("android.")
                || clsName.startsWith("com.android.");
    }

    private static boolean isAnnotatedMethod(final Method method,
                                             final Class<? extends Annotation> annotation) {
        // must has annotation
        if (!method.isAnnotationPresent(annotation)) {
            return false;
        }
        // must be public
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        // must not static
        if (Modifier.isStatic(method.getModifiers())) {
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

    public static Set<Subscriber> findSubscriber(final Object target,
                                                 final Class<? extends Annotation> annotation) {
        Class<?> clazz = target.getClass();
        final Set<Subscriber> subscribers = new HashSet<Subscriber>();
        while (!shouldSkipClass(clazz)) {
            final Method[] allMethods = clazz.getDeclaredMethods();
            System.out.println("findSubscriber() " + clazz.getSimpleName()
                    + " has " + allMethods.length + " methods");
            for (final Method method : allMethods) {
                if (isAnnotatedMethod(method, annotation)) {
                    subscribers.add(new Subscriber(method, target));
                }
            }
            // search more methods in super class
            clazz = clazz.getSuperclass();
        }
        return subscribers;
    }

    public static void dumpMethod(final Method method) {
        final StringBuilder builder = new StringBuilder();
        builder.append("------------------------------\n");
        builder.append("MethodName: ").append(method.getName()).append("\n");
        builder.append("ParameterTypes:{");
        for (Class<?> cls : method.getParameterTypes()) {
            builder.append(cls.getName()).append(", ");
        }
        builder.append("}\n");
        builder.append("GenericParameterTypes:{");
        for (Type cls : method.getGenericParameterTypes()) {
            builder.append(cls.getClass()).append(", ");
        }
        builder.append("}\n");
        builder.append("TypeParameters:{");
        for (TypeVariable<Method> cls : method.getTypeParameters()) {
            builder.append(cls.getName()).append(", ");
        }
        builder.append("}\n");
        builder.append("DeclaredAnnotations:{");
        for (Annotation cls : method.getDeclaredAnnotations()) {
            builder.append(cls).append(", ");
        }
        builder.append("}\n");
        builder.append("Annotations:{");
        for (Annotation cls : method.getAnnotations()) {
            builder.append(cls).append(", ");
        }
        builder.append("}\n");
        builder.append("ExceptionTypes:{");
        for (Class<?> cls : method.getExceptionTypes()) {
            builder.append(cls.getName()).append(", ");
            ;
        }
        builder.append("}\n");
        builder.append("ReturnType: ").append(method.getReturnType());
        builder.append("\nGenericReturnType: ").append(method.getGenericReturnType());
        builder.append("\nDeclaringClass: ").append(method.getDeclaringClass());
        builder.append("\n");

        System.out.println(builder.toString());
    }
}
