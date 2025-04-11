package com.contextualmocker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

class ContextualInvocationHandler implements InvocationHandler {

    private static final ThreadLocal<Method> lastInvokedMethod = new ThreadLocal<>();
    private static final ThreadLocal<Object[]> lastInvokedArgs = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> stubbingInProgress = ThreadLocal.withInitial(() -> false);

    public static Method consumeLastInvokedMethod() {
        Method m = lastInvokedMethod.get();
        lastInvokedMethod.remove();
        return m;
    }

    public static Object[] consumeLastInvokedArgs() {
        Object[] a = lastInvokedArgs.get();
        lastInvokedArgs.remove();
        return a;
    }

    public static void setStubbingInProgress(boolean inProgress) {
        stubbingInProgress.set(inProgress);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        lastInvokedMethod.set(method);
        lastInvokedArgs.set(args != null ? args.clone() : new Object[0]);

        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        ContextID contextId = ContextHolder.getContext();
        // Make a copy of the arguments to avoid modification issues
        Object[] safeArgs = args != null ? args.clone() : new Object[0];
        
        if (!stubbingInProgress.get() || contextId != null) {
            // Save any matchers in the ThreadLocal context that were used to invoke this method
            List<ArgumentMatcher<?>> matchers = MatcherContext.consumeMatchers();
            InvocationRecord record = new InvocationRecord(proxy, method, safeArgs, contextId, stubbingInProgress.get(), matchers);
            MockRegistry.recordInvocation(record);
        }

        // Retrieve the current state and find a matching stubbing rule
        Object currentState = MockRegistry.getState(proxy, contextId);
        StubbingRule rule = MockRegistry.findStubbingRule(proxy, contextId, method, safeArgs, currentState);

        if (rule != null) {
            // Apply the rule to get the result, and update state if needed
            Object result = rule.apply(contextId, proxy, method, safeArgs);
            Object nextState = rule.getNextState();
            if (nextState != null) {
                MockRegistry.setState(proxy, contextId, nextState);
            }
            return result;
        } else {
            // No matching rule, return default value
            return getDefaultValue(method.getReturnType());
        }
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "ContextualMock<" + proxy.getClass().getInterfaces()[0].getSimpleName() + ">@" + Integer.toHexString(System.identityHashCode(proxy));
            default:
                throw new UnsupportedOperationException("Unsupported Object method: " + method.getName());
        }
    }

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
            if (type == void.class) return null;
        }
        return null;
    }
}
