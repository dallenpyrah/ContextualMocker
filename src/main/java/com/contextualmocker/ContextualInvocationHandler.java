package com.contextualmocker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

class ContextualInvocationHandler implements InvocationHandler {

    // No state needed here, interacts purely with MockRegistry and ContextHolder

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        // Handle special methods like Object.equals/hashCode/toString
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        ContextID contextId = ContextHolder.getContext(); // Get context for this thread

        // 1. Record the invocation
        InvocationRecord record = new InvocationRecord(proxy, method, args, contextId);
        MockRegistry.recordInvocation(record);

        // 2. Find a matching stubbing rule
        StubbingRule rule = MockRegistry.findStubbingRule(proxy, contextId, method, args);

        // 3. Apply the rule or return default
        if (rule != null) {
            return rule.apply(contextId, proxy, method, args);
        } else {
            // No matching rule found, return default value
            return getDefaultValue(method.getReturnType());
        }
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "equals":
                // Proxy equality is reference equality
                return proxy == args[0];
            case "hashCode":
                // Use system identity hash code for proxies
                return System.identityHashCode(proxy);
            case "toString":
                // Provide a meaningful toString for the mock proxy
                return "ContextualMock<" + proxy.getClass().getInterfaces()[0].getSimpleName() + ">@" + Integer.toHexString(System.identityHashCode(proxy));
            default:
                // Should not happen for Object methods
                throw new UnsupportedOperationException("Unsupported Object method: " + method.getName());
        }
    }

    // Duplicated from StubbingRule for convenience, could be refactored to a common utility
    private static Object getDefaultValue(Class<?> type) {
        if (type.isPrimitive()) {
            if (type == boolean.class) return false;
            if (type == byte.class) return (byte) 0;
            if (type == short.class) return (short) 0;
            if (type == int.class) return 0;
            if (type == long.class) return 0L;
            if (type == float.class) return 0.0f;
            if (type == double.class) return 0.0d;
            if (type == char.class) return '\u0000';
            if (type == void.class) return null; // Void methods return null implicitly
        }
        return null; // Default for objects
    }
}
