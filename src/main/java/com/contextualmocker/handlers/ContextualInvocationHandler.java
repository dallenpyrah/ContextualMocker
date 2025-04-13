package com.contextualmocker.handlers;
import com.contextualmocker.core.ContextID;
import com.contextualmocker.core.ContextHolder;
import com.contextualmocker.matchers.ArgumentMatcher;
import com.contextualmocker.matchers.MatcherContext;
import com.contextualmocker.core.InvocationRecord;
import com.contextualmocker.core.MockRegistry;
import com.contextualmocker.core.StubbingRule;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;

public class ContextualInvocationHandler implements InvocationHandler {

    private static final ThreadLocal<List<ArgumentMatcher<?>>> lastInvokedMatchers = new ThreadLocal<>();

    public static List<ArgumentMatcher<?>> consumeLastInvokedMatchers() {
        List<ArgumentMatcher<?>> matchers = lastInvokedMatchers.get();
        lastInvokedMatchers.remove();
        return matchers;
    }

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
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        lastInvokedMethod.set(method);
        lastInvokedArgs.set(args != null ? args.clone() : new Object[0]);

        ContextID contextId = ContextHolder.getContext();
        Object[] safeArgs = args != null ? args.clone() : new Object[0];

        if (stubbingInProgress.get()) {
            List<ArgumentMatcher<?>> matchers = MatcherContext.consumeMatchers();
            lastInvokedMatchers.set(matchers);
        } else {
            List<ArgumentMatcher<?>> matchers = MatcherContext.consumeMatchers();
            InvocationRecord record = new InvocationRecord(proxy, method, safeArgs, contextId, stubbingInProgress.get(), matchers);
            MockRegistry.recordInvocation(record);
        }

        Object currentState = contextId != null ? MockRegistry.getState(proxy, contextId) : null;
        StubbingRule rule = MockRegistry.findStubbingRule(proxy, contextId, method, safeArgs, currentState);

        if (rule != null) {
            Object result = rule.apply(contextId, proxy, method, safeArgs);
            Object nextState = rule.getNextState();
            if (nextState != null && contextId != null) {
                MockRegistry.setState(proxy, contextId, nextState);
            }
            return result;
        } else {
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
                Class<?>[] interfaces = proxy.getClass().getInterfaces();
                String typeName;
                if (interfaces.length > 0) {
                    typeName = interfaces[0].getSimpleName();
                } else {
                    typeName = proxy.getClass().getSuperclass() != null
                        ? proxy.getClass().getSuperclass().getSimpleName()
                        : proxy.getClass().getSimpleName();
                }
                return "ContextualMock<" + typeName + ">@" + Integer.toHexString(System.identityHashCode(proxy));
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
