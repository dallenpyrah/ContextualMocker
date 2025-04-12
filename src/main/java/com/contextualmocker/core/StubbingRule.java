package com.contextualmocker.core;
import com.contextualmocker.matchers.ArgumentMatcher;
import com.contextualmocker.core.ContextID;
import com.contextualmocker.handlers.ContextualAnswer;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StubbingRule {
    private static final Logger logger = LoggerFactory.getLogger(StubbingRule.class);
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

    // TTL/expiration fields
    private final long creationTimeMillis;
    private final long ttlMillis; // <=0 means no expiration

    private StubbingRule(Method method, Object[] expectedArguments, ArgumentMatcher<?>[] argumentMatchers, ContextualAnswer<?> answer, Object returnValue, Throwable throwable, Object requiredState, Object nextState, long ttlMillis) {
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
        this.creationTimeMillis = System.currentTimeMillis();
        this.ttlMillis = ttlMillis;
    }

    static Builder builder(Method method) {
        return new Builder(method);
    }

    public static class Builder {
        private final Method method;
        private Object[] expectedArguments;
        private ArgumentMatcher<?>[] argumentMatchers;
        private ContextualAnswer<?> answer;
        private Object returnValue;
        private Throwable throwable;
        private Object requiredState;
        private Object nextState;
        private long ttlMillis = 0; // default: no expiration

        public Builder(Method method) {
            this.method = method;
        }

        public Builder expectedArguments(Object[] args) {
            this.expectedArguments = args;
            return this;
        }

        public Builder argumentMatchers(ArgumentMatcher<?>[] matchers) {
            this.argumentMatchers = matchers;
            return this;
        }

        public Builder answer(ContextualAnswer<?> answer) {
            this.answer = answer;
            return this;
        }

        public Builder returnValue(Object value) {
            this.returnValue = value;
            return this;
        }

        public Builder throwable(Throwable t) {
            this.throwable = t;
            return this;
        }

        public Builder requiredState(Object state) {
            this.requiredState = state;
            return this;
        }

        public Builder nextState(Object state) {
            this.nextState = state;
            return this;
        }

        /**
         * Set the time-to-live (TTL) for this stubbing rule in milliseconds.
         * If not set or <=0, the rule will not expire.
         * @param ttlMillis Time-to-live in milliseconds.
         * @return This builder.
         */
        public Builder ttlMillis(long ttlMillis) {
            this.ttlMillis = ttlMillis;
            return this;
        }

        public StubbingRule build() {
            return new StubbingRule(method, expectedArguments, argumentMatchers, answer, returnValue, throwable, requiredState, nextState, ttlMillis);
        }
    }

    boolean matches(Method invokedMethod, Object[] invokedArguments, Object currentState) {
        // 1. Method Check
        if (!method.equals(invokedMethod)) {
            return false;
        }

        // 2. State Check
        if (!Objects.equals(this.requiredState, currentState)) {
            return false;
        }

        // 3. Argument Count Check
        Object[] actualArgs = (invokedArguments == null) ? new Object[0] : invokedArguments;
        int expectedArgsLength = (argumentMatchers != null) ? argumentMatchers.length : expectedArguments.length;
        if (actualArgs.length != expectedArgsLength) {
            return false;
        }

        // 4. Argument Value/Matcher Check
        if (argumentMatchers != null && argumentMatchers.length > 0) {
            // Use matcher logic for all arguments
            for (int i = 0; i < argumentMatchers.length; i++) {
                ArgumentMatcher<?> matcher = argumentMatchers[i];
                if (matcher != null) {
                    // Check if matcher is AnyMatcher (always matches)
                    if (matcher instanceof com.contextualmocker.matchers.AnyMatcher) {
                        continue;
                    }
                    @SuppressWarnings("unchecked")
                    ArgumentMatcher<Object> objectMatcher = (ArgumentMatcher<Object>) matcher;
                    if (!objectMatcher.matches(actualArgs[i])) {
                        return false;
                    }
                } else {
                    // If matcher is null, fallback to deepEquals for this argument
                    if (!Objects.deepEquals(expectedArguments[i], actualArgs[i])) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            // No matchers: use deepEquals for all arguments
            boolean argsMatch = Arrays.deepEquals(expectedArguments, actualArgs);
            if (!argsMatch) {
                if (logger.isDebugEnabled()) {
                    logger.debug("deepEquals failed: ");
                    logger.debug("  Expected: {} (Hash: {})", Arrays.toString(expectedArguments), Arrays.hashCode(expectedArguments));
                    logger.debug("  Actual:   {} (Hash: {})", Arrays.toString(actualArgs), Arrays.hashCode(actualArgs));
                    if (expectedArguments.length > 0 && actualArgs.length > 0) {
                        logger.debug("  Expected[0]: '{}' (Class: {}, Hash: {})",
                            expectedArguments[0], expectedArguments[0].getClass().getName(), Objects.hashCode(expectedArguments[0]));
                        logger.debug("  Actual[0]:   '{}' (Class: {}, Hash: {})",
                            actualArgs[0], actualArgs[0].getClass().getName(), Objects.hashCode(actualArgs[0]));
                    }
                }
            }
            return argsMatch;
        }
    }

    @SuppressWarnings("unchecked")
    public Object apply(ContextID contextId, Object mock, Method invokedMethod, Object[] invokedArguments) throws Throwable {
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

    public Object getNextState() {
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

    /**
     * Checks if this stubbing rule has expired based on its TTL.
     * @return true if the rule has a positive TTL and the current time is past the creation time plus TTL, false otherwise.
     */
    public boolean isExpired() {
        if (ttlMillis <= 0) {
            return false; // No expiration
        }
        long expiryTime = creationTimeMillis + ttlMillis;
        return System.currentTimeMillis() > expiryTime;
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
        if (ttlMillis > 0) {
            sb.append(", ttlMillis=").append(ttlMillis);
            sb.append(", creationTimeMillis=").append(creationTimeMillis);
        }
        sb.append("}");

        return sb.toString();
    }
}
