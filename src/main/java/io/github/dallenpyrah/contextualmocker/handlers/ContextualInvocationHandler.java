package io.github.dallenpyrah.contextualmocker.handlers;
import io.github.dallenpyrah.contextualmocker.core.ContextID;
import io.github.dallenpyrah.contextualmocker.core.ContextHolder;
import io.github.dallenpyrah.contextualmocker.core.DefaultValueProvider;
import io.github.dallenpyrah.contextualmocker.matchers.ArgumentMatcher;
import io.github.dallenpyrah.contextualmocker.matchers.MatcherContext;
import io.github.dallenpyrah.contextualmocker.core.InvocationRecord;
import io.github.dallenpyrah.contextualmocker.core.MockRegistry;
import io.github.dallenpyrah.contextualmocker.core.StubbingRule;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class ContextualInvocationHandler implements InvocationHandler {

    private static final Map<String, ObjectMethodHandler> objectMethodHandlers = new HashMap<>();
    
    static {
        objectMethodHandlers.put("equals", new EqualsHandler());
        objectMethodHandlers.put("hashCode", new HashCodeHandler());
        objectMethodHandlers.put("toString", new ToStringHandler());
    }

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
            return DefaultValueProvider.getDefaultValue(method.getReturnType());
        }
    }

    private Object handleObjectMethod(Object proxy, Method method, Object[] args) {
        ObjectMethodHandler handler = objectMethodHandlers.get(method.getName());
        if (handler != null) {
            return handler.handle(proxy, method, args);
        }
        throw new UnsupportedOperationException("Unsupported Object method: " + method.getName());
    }

}
