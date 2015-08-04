package com.mcxiaoke.bus.method;

import com.mcxiaoke.bus.MethodInfo;

import java.util.Set;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 18:14
 */
public interface MethodFinder {

    Set<MethodInfo> findSubscriberMethods(final Class<?> targetClass);
}
