package com.mcxiaoke.bus.method;

import com.mcxiaoke.bus.Bus;
import com.mcxiaoke.bus.MethodInfo;

import java.util.Set;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 18:15
 */
public class AnnotationMethodFinder implements MethodFinder {


    @Override
    public Set<MethodInfo> find(final Bus bus, final Class<?> targetClass) {
        return MethodHelper.findSubscriberMethodsByAnnotation(targetClass);
    }
}
