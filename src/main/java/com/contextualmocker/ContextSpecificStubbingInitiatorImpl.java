package com.contextualmocker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;

class ContextSpecificStubbingInitiatorImpl<T> implements ContextualMocker.ContextSpecificStubbingInitiator<T> {
    private final T mock;

    ContextSpecificStubbingInitiatorImpl(T mock) {
        this.mock = mock;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> ContextualMocker.OngoingContextualStubbing<T, R> when(R methodCall) {
        Objects.requireNonNull(ContextHolder.getContext(), "ContextID must be set before stubbing");
        MethodCaptureInvocationHandler<T, R> handler = new MethodCaptureInvocationHandler<>(mock);
        T proxy = (T) Proxy.newProxyInstance(
                mock.getClass().getClassLoader(),
                mock.getClass().getInterfaces(),
                handler
        );
        return new OngoingContextualStubbingImpl<>(mock, ContextHolder.getContext(), handler);
    }

    private static class MethodCaptureInvocationHandler<T, R> implements InvocationHandler {
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

    private static class OngoingContextualStubbingImpl<T, R> implements ContextualMocker.OngoingContextualStubbing<T, R> {
        private final T mock;
        private final ContextID contextId;
        private final MethodCaptureInvocationHandler<T, R> handler;

        OngoingContextualStubbingImpl(T mock, ContextID contextId, MethodCaptureInvocationHandler<T, R> handler) {
            this.mock = mock;
            this.contextId = contextId;
            this.handler = handler;
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenReturn(R value) {
            registerRule(StubbingRule.forReturnValue(handler.getMethod(), handler.getArgs(), value));
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenThrow(Throwable throwable) {
            registerRule(StubbingRule.forThrowable(handler.getMethod(), handler.getArgs(), throwable));
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        @Override
        public ContextualMocker.ContextSpecificStubbingInitiator<T> thenAnswer(ContextualAnswer<R> answer) {
            registerRule(StubbingRule.forAnswer(handler.getMethod(), handler.getArgs(), answer));
            return new ContextSpecificStubbingInitiatorImpl<>(mock);
        }

        private void registerRule(StubbingRule rule) {
            MockRegistry.addStubbingRule(mock, contextId, rule);
        }
    }
}