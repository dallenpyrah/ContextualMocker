package com.contextualmocker;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

class StubbingRule {
    private final Method method;
    private final Object[] expectedArguments;
    private final ArgumentMatcher<?>[] argumentMatchers;
    private final ContextualAnswer<?> answer;
    private final Object returnValue;
    private final Throwable throwable;
    private final boolean hasReturnValue;
    private final boolean hasThrowable;
    private final boolean hasAnswer;
    private final Object requiredState;
    private final Object nextState;

    private StubbingRule(Method method, Object[] expectedArguments, ArgumentMatcher<?>[] argumentMatchers, ContextualAnswer<?> answer, Object returnValue, Throwable throwable, Object requiredState, Object nextState) {
        this.method = Objects.requireNonNull(method, "Method cannot be null");
        this.expectedArguments = expectedArguments == null ? new Object[0] : expectedArguments.clone();
        this.argumentMatchers = argumentMatchers;
        this.answer = answer;
        this.returnValue = returnValue;
        this.throwable = throwable;
        this.hasAnswer = answer != null;
        this.hasReturnValue = returnValue != null && !hasAnswer;
        this.hasThrowable = throwable != null && !hasAnswer && !hasReturnValue;
        this.requiredState = requiredState;
        this.nextState = nextState;
    }

    static Builder builder(Method method) {
        return new Builder(method);
    }

    static class Builder {
        private final Method method;
        private Object[] expectedArguments;
        private ArgumentMatcher<?>[] argumentMatchers;
        private ContextualAnswer<?> answer;
        private Object returnValue;
        private Throwable throwable;
        private Object requiredState;
        private Object nextState;

        Builder(Method method) {
            this.method = method;
        }

        Builder expectedArguments(Object[] args) {
            this.expectedArguments = args;
            return this;
        }

        Builder argumentMatchers(ArgumentMatcher<?>[] matchers) {
            this.argumentMatchers = matchers;
            return this;
        }

        Builder answer(ContextualAnswer<?> answer) {
            this.answer = answer;
            return this;
        }

        Builder returnValue(Object value) {
            this.returnValue = value;
            return this;
        }

        Builder throwable(Throwable t) {
            this.throwable = t;
            return this;
        }

        Builder requiredState(Object state) {
            this.requiredState = state;
            return this;
        }

        Builder nextState(Object state) {
            this.nextState = state;
            return this;
        }

        StubbingRule build() {
            return new StubbingRule(method, expectedArguments, argumentMatchers, answer, returnValue, throwable, requiredState, nextState);
        }
    }

    boolean matches(Method invokedMethod, Object[] invokedArguments, Object currentState) {
        if (!method.equals(invokedMethod)) {
            return false;
        }
        if (requiredState != null && !Objects.equals(requiredState, currentState)) {
            return false;
        }
        if (argumentMatchers != null) {
            if (invokedArguments == null) {
                invokedArguments = new Object[0];
            }
            // If lengths don't match, we can't properly apply matchers
            if (argumentMatchers.length != invokedArguments.length) return false;
            
            for (int i = 0; i < argumentMatchers.length; i++) {
                @SuppressWarnings("unchecked")
                ArgumentMatcher<Object> matcher = (ArgumentMatcher<Object>) argumentMatchers[i];
                // Ensure matcher is not null before trying to match
                if (matcher != null && !matcher.matches(invokedArguments[i])) {
                    return false;
                }
            }
            return true;
        }
        return Arrays.equals(expectedArguments, invokedArguments);
    }

    Object apply(ContextID contextId, Object mock, Method invokedMethod, Object[] invokedArguments) throws Throwable {
        if (hasAnswer) {
            return answer.answer(contextId, mock, invokedMethod, invokedArguments);
        } else if (hasThrowable) {
            throw throwable;
        } else if (hasReturnValue) {
            return returnValue;
        } else {
            return getDefaultValue(method.getReturnType());
        }
    }

    Object getNextState() {
        return nextState;
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
        }
        return null;
    }

    @Override
    public String toString() {
        String action;
        if (hasAnswer) action = "answer=" + answer;
        else if (hasThrowable) action = "throwable=" + throwable;
        else if (hasReturnValue) action = "returnValue=" + returnValue;
        else action = "default value";

        StringBuilder sb = new StringBuilder("StubbingRule{");
        sb.append("method=").append(method.getName());
        sb.append(", expectedArguments=").append(Arrays.toString(expectedArguments));
        
        if (argumentMatchers != null) {
            sb.append(", argumentMatchers=").append(Arrays.toString(argumentMatchers));
        }
        
        sb.append(", requiredState=").append(requiredState);
        sb.append(", nextState=").append(nextState);
        sb.append(", action=").append(action);
        sb.append("}");
        
        return sb.toString();
    }
}