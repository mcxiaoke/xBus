package com.mcxiaoke.bus.method;

import com.mcxiaoke.bus.MethodInfo;

import java.lang.reflect.Method;

/**
 * User: mcxiaoke
 * Date: 15/8/4
 * Time: 18:32
 */
interface MethodConverter {

    MethodInfo convert(final Method method);
}
