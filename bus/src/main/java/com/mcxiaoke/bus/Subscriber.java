package com.mcxiaoke.bus;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 15:43
 */
class Subscriber {
    public final MethodInfo method;
    public final Object target;
    public final Class<?> targetType;
    public final Class<?> eventType;
    public final Bus.EventMode mode;
    public final String name;

    public Subscriber(final MethodInfo method, final Object target) {
        this.method = method;
        this.target = target;
        this.eventType = method.eventType;
        this.targetType = method.targetType;
        this.mode = method.mode;
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
                + "(" + eventType.getSimpleName() + ")"
                + "-" + method.mode.name();
    }


}
