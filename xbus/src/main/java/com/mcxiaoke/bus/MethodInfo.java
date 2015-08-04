package com.mcxiaoke.bus;

import java.lang.reflect.Method;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:44
 */
public class MethodInfo {
    public final Method method;
    public final Class<?> targetType;
    public final Class<?> eventType;
    public final String name;
    public final EventMode mode;

    public MethodInfo(final Method method, final Class<?> targetClass, final EventMode mode) {
        this.method = method;
        this.targetType = targetClass;
        this.eventType = method.getParameterTypes()[0];
        this.mode = mode;
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
