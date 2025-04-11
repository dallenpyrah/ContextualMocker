package com.contextualmocker;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.List;
import java.util.Objects;

class ContextSpecificVerificationInitiatorImpl<T> implements ContextualMocker.ContextSpecificVerificationInitiator<T> {
    private final T mock;

    ContextSpecificVerificationInitiatorImpl(T mock) {
        this.mock = mock;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T verify(ContextualMocker.ContextualVerificationMode mode) {
        Objects.requireNonNull(ContextHolder.getContext(), "ContextID must be set before verification");
        VerificationMethodCaptureHandler<T> handler = new VerificationMethodCaptureHandler<>(mock, mode, ContextHolder.getContext());
        return (T) Proxy.newProxyInstance(
                mock.getClass().getClassLoader(),
                mock.getClass().getInterfaces(),
                handler
        );
    }

    private static class VerificationMethodCaptureHandler<T> implements InvocationHandler {
        private final T mock;
        private final ContextualMocker.ContextualVerificationMode mode;
        private final ContextID contextId;
        private Method method;
        private Object[] args;

        VerificationMethodCaptureHandler(T mock, ContextualMocker.ContextualVerificationMode mode, ContextID contextId) {
            this.mock = mock;
            this.mode = mode;
            this.contextId = contextId;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) {
            if (this.method == null) {
                this.method = method;
                this.args = args != null ? args.clone() : new Object[0];
                List<ArgumentMatcher<?>> matchers = MatcherContext.consumeMatchers();
                List<InvocationRecord> invocations = MockRegistry.getInvocationRecords(mock, contextId);
                int matchCount = 0;
                for (InvocationRecord record : invocations) {
                    if (record.getMethod().equals(method)) {
                        if (!matchers.isEmpty()) {
                            boolean allMatch = true;
                            Object[] actualArgs = record.getArguments();
                            if (actualArgs.length != matchers.size()) continue;
                            for (int i = 0; i < matchers.size(); i++) {
                                if (!matchers.get(i).matches(actualArgs[i])) {
                                    allMatch = false;
                                    break;
                                }
                            }
                            if (allMatch) matchCount++;
                        } else if (Objects.deepEquals(record.getArguments(), args)) {
                            matchCount++;
                        }
                    }
                }
                if (mode instanceof ContextualMocker.TimesVerificationMode) {
                    ((ContextualMocker.TimesVerificationMode) mode).verifyCount(matchCount, method, args);
                } else if (mode instanceof ContextualMocker.AtLeastVerificationMode) {
                    ((ContextualMocker.AtLeastVerificationMode) mode).verifyCount(matchCount, method, args);
                } else if (mode instanceof ContextualMocker.AtMostVerificationMode) {
                    ((ContextualMocker.AtMostVerificationMode) mode).verifyCount(matchCount, method, args);
                }
            }
            return getDefaultValue(method.getReturnType());
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
}