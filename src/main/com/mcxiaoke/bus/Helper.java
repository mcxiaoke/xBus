package com.mcxiaoke.bus;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

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
        // must not static
        if (Modifier.isStatic(method.getModifiers())) {
            return false;
        }
        // must be public
        if (!Modifier.isPublic(method.getModifiers())) {
            return false;
        }
        // must has only one parameter
        if (method.getParameterTypes().length != 1) {
            return false;
        }

        return true;
    }

    public static List<Method> findAnnotatedMethods(final Class<?> type,
                                                    final Class<? extends Annotation> annotation) {
        Class<?> clazz = type;
        final List<Method> methods = new ArrayList<Method>();
        while (!shouldSkipClass(clazz)) {
            final Method[] allMethods = clazz.getDeclaredMethods();
            for (final Method method : allMethods) {
                if (isAnnotatedMethod(method, annotation)) {
                    methods.add(method);
                }
            }
            // search more methods in super class
            clazz = clazz.getSuperclass();
        }
        return methods;
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
            builder.append(cls.getTypeName()).append(", ");
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
