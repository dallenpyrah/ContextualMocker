package com.contextualmocker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

class MethodCaptureInvocationHandler<T, R> implements InvocationHandler {
    private Method method;
    private Object[] args;
    private final T mock;

    MethodCaptureInvocationHandler(T mock) {
        this.mock = mock;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        if (this.method == null) {
            this.method = method;
            this.args = args != null ? args.clone() : new Object[0];
        }
        return getDefaultValue(method.getReturnType());
    }

    Method getMethod() {
        return method;
    }

    Object[] getArgs() {
        return args;
    }

    private Object getDefaultValue(Class<?> type) {
        if (!type.isPrimitive()) return null;
        if (type == boolean.class) return false;
        if (type == byte.class) return (byte) 0;
        if (type == short.class) return (short) 0;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == float.class) return 0.0f;
        if (type == double.class) return 0.0d;
        if (type == char.class) return '\u0000';
        return null;
    }
}