package com.contextualmocker.handlers;

import com.contextualmocker.core.ContextHolder;
import com.contextualmocker.core.ContextID;
import com.contextualmocker.core.MockRegistry;
import com.contextualmocker.core.StubbingRule;
import com.contextualmocker.core.InvocationRecord;
import com.contextualmocker.matchers.MatcherContext;
import com.contextualmocker.matchers.ArgumentMatcher;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Objects;

/**
 * Invocation handler for spy objects that delegates to real implementations
 * when no stubbing rule is found, but allows selective stubbing of methods.
 */
public class SpyInvocationHandler implements InvocationHandler {
    private final Object realObject;
    private static final ThreadLocal<List<ArgumentMatcher<?>>> lastInvokedMatchers = new ThreadLocal<>();
    private static final ThreadLocal<Method> lastInvokedMethod = new ThreadLocal<>();
    private static final ThreadLocal<Object[]> lastInvokedArgs = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> stubbingInProgress = ThreadLocal.withInitial(() -> false);

    public SpyInvocationHandler(Object realObject) {
        this.realObject = Objects.requireNonNull(realObject, "Real object cannot be null");
    }

    public static List<ArgumentMatcher<?>> consumeLastInvokedMatchers() {
        List<ArgumentMatcher<?>> matchers = lastInvokedMatchers.get();
        lastInvokedMatchers.remove();
        return matchers;
    }

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
        // Handle Object methods first
        if (method.getDeclaringClass() == Object.class) {
            return handleObjectMethod(proxy, method, args);
        }

        lastInvokedMethod.set(method);
        lastInvokedArgs.set(args != null ? args.clone() : new Object[0]);

        ContextID currentContext = ContextHolder.getContext();
        Object[] safeArgs = args != null ? args.clone() : new Object[0];

        if (stubbingInProgress.get()) {
            List<ArgumentMatcher<?>> matchers = MatcherContext.consumeMatchers();
            lastInvokedMatchers.set(matchers);
        } else {
            List<ArgumentMatcher<?>> matchers = MatcherContext.consumeMatchers();
            InvocationRecord record = new InvocationRecord(proxy, method, safeArgs, currentContext, stubbingInProgress.get(), matchers);
            MockRegistry.recordInvocation(record);
        }

        Object currentState = currentContext != null ? MockRegistry.getState(proxy, currentContext) : null;
        StubbingRule rule = MockRegistry.findStubbingRule(proxy, currentContext, method, safeArgs, currentState);

        if (rule != null && !rule.isExpired()) {
            Object result = rule.apply(currentContext, proxy, method, safeArgs);
            Object nextState = rule.getNextState();
            if (nextState != null && currentContext != null) {
                MockRegistry.setState(proxy, currentContext, nextState);
            }
            return result;
        } else {
            // No stubbing rule found, delegate to real object
            try {
                return method.invoke(realObject, args);
            } catch (java.lang.reflect.InvocationTargetException e) {
                throw e.getCause();
            }
        }
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        switch (method.getName()) {
            case "equals":
                return proxy == args[0];
            case "hashCode":
                return System.identityHashCode(proxy);
            case "toString":
                return "Spy of " + realObject.getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(proxy));
            default:
                throw new UnsupportedOperationException("Unsupported Object method: " + method.getName());
        }
    }
}