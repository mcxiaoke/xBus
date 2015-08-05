package com.mcxiaoke.bus.method;

import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.MethodInfo;

import java.util.Set;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 18:17
 */
public class NamedMethodFinder implements MethodFinder {
    private static final String NAME_DEFAULT = "onEvent";

    private final String name;

    public NamedMethodFinder() {
        this(NAME_DEFAULT);
    }

    public NamedMethodFinder(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("invalid name");
        }
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public Set<MethodInfo> find(final Bus bus, final Class<?> targetClass) {
        return MethodHelper.findSubscriberMethodsByName(targetClass, name, bus.isStrictMode());
    }
}
